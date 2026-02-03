package com.pms.validation.service.processing;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.entity.InvalidTradeEntity;
import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.proto.TradeEventProto;
import com.pms.validation.repository.InvalidTradeRepository;
import com.pms.validation.repository.ValidationOutboxRepository;
import com.pms.validation.mapper.ProtoDTOMapper;
import com.pms.validation.service.domain.TradeIdempotencyService;
import com.pms.rttm.client.clients.RttmClient;
import com.pms.rttm.client.dto.TradeEventPayload;
import com.pms.rttm.client.dto.ErrorEventPayload;
import com.pms.rttm.client.enums.EventType;
import com.pms.rttm.client.enums.EventStage;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ValidationBatchProcessingService {

    @Autowired
    private TradeIdempotencyService idempotencyService;

    // kept for compatibility; direct persistence is handled in this batch service
    // now

    @Autowired
    private ValidationCore validationCore;

    @Autowired
    private ValidationOutboxRepository validationOutboxRepository;

    @Autowired
    private InvalidTradeRepository invalidTradeRepository;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RttmClient rttmClient;

    @Value("${app.outgoing-valid-trades-topic}")
    private String validTradesTopic;

    @Value("${app.outgoing-invalid-trades-topic}")
    private String invalidTradesTopic;

    @Value("${spring.application.name}")
    private String serviceName;

    @Transactional
    public void processBatch(List<TradeEventProto> messages, int partition, String topic, List<Long> offsets,
            String consumerGroup) {
        log.info("Processing validation batch of {} trades.", messages.size());

        List<TradeDto> dtos = messages.stream()
                .map(ProtoDTOMapper::toDto)
                .collect(Collectors.toList());

        // collect ids which were successfully processed in this transaction
        List<UUID> successfulIds = new ArrayList<>();
        // ids for which we acquired a Redis PROCESSING reservation
        List<UUID> reservedIds = new ArrayList<>();

        // Collect entities for batch persistence
        List<ValidationOutboxEntity> outboxToSave = new ArrayList<>();
        List<InvalidTradeEntity> invalidToSave = new ArrayList<>();

        for (int i = 0; i < dtos.size(); i++) {
            TradeDto dto = dtos.get(i);
            Long offset = (offsets != null && i < offsets.size()) ? offsets.get(i) : 0L;
            // idempotency check
            if (dto.getTradeId() == null) {
                log.warn("Skipping trade with null id");
                continue;
            }

            // Try to reserve processing for this trade to avoid race with other consumers
            if (idempotencyService.isDone(dto.getTradeId())) {
                log.info("Trade already done, skipping | tradeId={}", dto.getTradeId());
                continue;
            }

            boolean reserved = idempotencyService.tryStartProcessing(dto.getTradeId());
            if (!reserved) {
                log.info("Trade already being processed by another worker, skipping | tradeId={}", dto.getTradeId());
                continue;
            }
            reservedIds.add(dto.getTradeId());

            try {
                // Evaluate rules and build entities (do not persist here)
                ValidationDecision decision = validationCore.evaluate(dto);

                if (decision.isValid()) {
                    outboxToSave.add(decision.getOutboxEntity());
                    sendTradeValidationEvent(dto, true, null, partition, offset, topic, consumerGroup);
                } else {
                    invalidToSave.add(decision.getInvalidEntity());
                    // Here you're sending invalid trades in rttm.trade.events topic which is meant
                    // only for valid trades,
                    // so send in invalid-trades-topic which the Invalid Outbox does.
                    // sendTradeValidationEvent(dto, false,
                    // decision.getInvalidEntity().getValidationErrors(), partition,
                    // offset, topic, consumerGroup);
                }

                successfulIds.add(dto.getTradeId());

            } catch (Exception ex) {
                log.error("Error evaluating trade {} in batch", dto.getTradeId(), ex);
                // Send error event to RTTM
                sendErrorEvent(dto, ex.getMessage(), partition, offset, topic, consumerGroup);
                throw ex; // let listener pause and handle
            }
        }

        // Persist entities in batch within the same transaction
        if (!outboxToSave.isEmpty()) {
            validationOutboxRepository.saveAll(outboxToSave);
            log.info("Saved {} outbox entries in batch.", outboxToSave.size());
        }

        if (!invalidToSave.isEmpty()) {
            invalidTradeRepository.saveAll(invalidToSave);
            log.info("Saved {} invalid trade entries in batch.", invalidToSave.size());
        }

        // Register synchronization to:
        // - mark DONE after commit for successfulIds
        // - clear PROCESSING reservation for reservedIds if transaction rolled back or
        // the id was not successful
        if (!reservedIds.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (UUID id : successfulIds) {
                        try {
                            idempotencyService.markDone(id);
                        } catch (Exception ex) {
                            log.error("Failed to mark idempotency DONE for {}", id, ex);
                        }
                    }
                }

                @Override
                public void afterCompletion(int status) {
                    // If transaction did not commit, release PROCESSING reservations for those we
                    // reserved
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        for (UUID id : reservedIds) {
                            if (!successfulIds.contains(id)) {
                                try {
                                    idempotencyService.clearProcessing(id);
                                } catch (Exception ex) {
                                    log.error("Failed to clear PROCESSING for {}", id, ex);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Send trade validation event to RTTM
     */
    private void sendTradeValidationEvent(TradeDto trade, boolean valid, String errors, int partition, Long offset,
            String topic, String consumerGroup) {
        try {
            TradeEventPayload event = TradeEventPayload.builder()
                    .tradeId(trade.getTradeId().toString())
                    .topicName(topic)
                    .offsetValue(offset)
                    .partitionId(partition)
                    .consumerGroup(consumerGroup)
                    .serviceName(serviceName)
                    .eventType(EventType.TRADE_VALIDATED)
                    .eventStage(EventStage.VALIDATED)
                    .eventStatus("OK")
                    .sourceQueue(topic)
                    .targetQueue(validTradesTopic)
                    .message("Trade validation passed")
                    .build();

            rttmClient.sendTradeEvent(event);
            log.debug("Sent Trade event (Valid Trade) to RTTM for trade {}", trade.getTradeId());
        } catch (Exception ex) {
            log.warn("Failed to send trade validation event to RTTM: {}", ex.getMessage());
        }
    }

    /**
     * Send error event to RTTM when validation fails
     */
    private void sendErrorEvent(TradeDto trade, String errorMessage, int partition, Long offset, String topic,
            String consumerGroup) {
        try {
            ErrorEventPayload errorEvent = ErrorEventPayload.builder()
                    .tradeId(trade.getTradeId().toString())
                    .serviceName(serviceName)
                    .errorType("VALIDATION_ERROR")
                    .errorMessage(errorMessage)
                    .eventStage(EventStage.VALIDATED)
                    .build();

            rttmClient.sendErrorEvent(errorEvent);
            log.debug("Sent error event to RTTM for trade {}", trade.getTradeId());
        } catch (Exception ex) {
            log.warn("Failed to send error event to RTTM: {}", ex.getMessage());
        }
    }
}
