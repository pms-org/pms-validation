package com.pms.validation.service.processing;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.proto.TradeEventProto;
import com.pms.validation.mapper.ProtoDTOMapper;
import com.pms.validation.service.domain.TradeIdempotencyService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ValidationBatchProcessingService {

    @Autowired
    private TradeIdempotencyService idempotencyService;

    // kept for compatibility; direct persistence is handled in this batch service now

    @Autowired
    private ValidationCore validationCore;

    @Autowired
    private com.pms.validation.repository.ValidationOutboxRepository validationOutboxRepository;

    @Autowired
    private com.pms.validation.repository.InvalidTradeRepository invalidTradeRepository;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void processBatch(List<TradeEventProto> messages) {
        log.info("Processing validation batch of {} trades.", messages.size());

        List<TradeDto> dtos = messages.stream()
                .map(ProtoDTOMapper::toDto)
                .collect(Collectors.toList());

    // collect ids which were successfully processed in this transaction
    List<java.util.UUID> successfulIds = new ArrayList<>();
    // ids for which we acquired a Redis PROCESSING reservation
    List<java.util.UUID> reservedIds = new ArrayList<>();

        // Collect entities for batch persistence
        List<com.pms.validation.entity.ValidationOutboxEntity> outboxToSave = new ArrayList<>();
        List<com.pms.validation.entity.InvalidTradeEntity> invalidToSave = new ArrayList<>();

        for (TradeDto dto : dtos) {
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
                } else {
                    invalidToSave.add(decision.getInvalidEntity());
                }

                successfulIds.add(dto.getTradeId());

            } catch (Exception ex) {
                log.error("Error evaluating trade {} in batch", dto.getTradeId(), ex);
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
        // - clear PROCESSING reservation for reservedIds if transaction rolled back or the id was not successful
        if (!reservedIds.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (java.util.UUID id : successfulIds) {
                        try {
                            idempotencyService.markDone(id);
                        } catch (Exception ex) {
                            log.error("Failed to mark idempotency DONE for {}", id, ex);
                        }
                    }
                }

                @Override
                public void afterCompletion(int status) {
                    // If transaction did not commit, release PROCESSING reservations for those we reserved
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        for (java.util.UUID id : reservedIds) {
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

        try {
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/position-update", dtos);
            } else {
                log.debug("SimpMessagingTemplate not present, skipping websocket update");
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to send websocket update", ex);
        }
    }
}
