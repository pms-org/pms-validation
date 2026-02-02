package com.pms.validation.service.outbox;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pms.validation.entity.InvalidTradeEntity;
import com.pms.validation.proto.InvalidTradeEventProto;
import com.pms.validation.mapper.ProtoInvalidTradeEntityMapper;
import com.pms.validation.repository.InvalidTradeRepository;
import com.pms.validation.repository.DlqRepository;
import com.pms.validation.entity.DlqEntry;
import com.pms.rttm.client.clients.RttmClient;
import com.pms.rttm.client.dto.DlqEventPayload;
import com.pms.rttm.client.enums.EventStage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvalidTradeOutboxEventProcessor {

    private final InvalidTradeRepository invalidTradeRepo;

    private final AdaptiveBatchSizer batchSizer;

    private final KafkaTemplate<String, InvalidTradeEventProto> invalidTradeKafkaTemplate;

    private final DlqRepository dlqRepository;

    private final RttmClient rttmClient;

    @Value("${app.outgoing-invalid-trades-topic}")
    private String invalidTradesTopic;

    @Value("${app.incoming-trades-topic}")
    private String incomingTradesTopic;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @Value("${spring.application.name}")
    private String serviceName;

    @Transactional
    public ProcessingResult<InvalidTradeEntity> dispatchOnce() {

        int limit = batchSizer.getCurrentSize();
        log.info("Invalid Trade batch limit: {}.", limit);

        List<InvalidTradeEntity> batch = invalidTradeRepo.findPendingWithPortfolioXactLock(limit);

        log.info("Fetched {} invalid trades from validation_invalid_trades.", batch.size());

        if (batch.isEmpty()) {
            batchSizer.reset();
            return ProcessingResult.success(List.of());
        }

        long start = System.currentTimeMillis();

        ProcessingResult<InvalidTradeEntity> result = process(batch);

        long duration = System.currentTimeMillis() - start;

        if (!result.systemFailure()) {
            batchSizer.adjust(duration, batch.size());
        }

        if (!result.successfulIds().isEmpty()) {
            invalidTradeRepo.markAsSent(result.successfulIds());
            log.info("Updated {} invalid trade events to SENT", result.successfulIds());
        }

        if (result.poisonPill() != null) {
            InvalidTradeEntity poison = result.poisonPill();
            invalidTradeRepo.markAsFailed(poison.getInvalidTradeOutboxId());
        }

        return result;
    }

    public ProcessingResult<InvalidTradeEntity> process(List<InvalidTradeEntity> events) {

        List<Long> successfulIds = new ArrayList<>();

        for (InvalidTradeEntity invalidTrade : events) {
            try {
                InvalidTradeEventProto proto = ProtoInvalidTradeEntityMapper.toProto(invalidTrade);

                var sendResult = invalidTradeKafkaTemplate.send(invalidTradesTopic, proto.getPortfolioId(), proto)
                        .get();
                var metadata = sendResult.getRecordMetadata();
                int partition = metadata.partition();
                long offset = metadata.offset();

                log.info("Invalid trade event with ID {} sent to kafka successfully.", proto.getTradeId());

                successfulIds.add(invalidTrade.getInvalidTradeOutboxId());

            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                log.error("Error sending invalid trade {} : {}", invalidTrade.getInvalidTradeOutboxId(),
                        e.getMessage());

                // Simple poison-pill classification: serialization and illegal argument are
                // considered poison
                boolean poison = cause instanceof org.apache.kafka.common.errors.SerializationException
                        || cause instanceof IllegalArgumentException
                        || cause instanceof com.fasterxml.jackson.core.JsonProcessingException;

                if (poison) {
                    try {
                        // attempt to persist payload to DLQ for later inspection
                        InvalidTradeEventProto proto = null;
                        try {
                            proto = ProtoInvalidTradeEntityMapper.toProto(invalidTrade);
                        } catch (Exception ex) {
                            // ignore, we'll persist empty payload if proto can't be built
                        }

                        byte[] payload = proto != null ? proto.toByteArray() : new byte[0];

                        DlqEntry entry = DlqEntry.builder()
                                .payload(payload)
                                .errorDetail(e.toString())
                                .build();

                        dlqRepository.save(entry);
                        log.warn("Persisted invalid trade {} to DLQ as id {}", invalidTrade.getInvalidTradeOutboxId(),
                                entry.getDlqEntryid());

                        // Send DLQ event to RTTM
                        sendDlqEventToRttm(invalidTrade, e.getMessage());

                        // mark as failed so dispatcher won't retry endlessly
                        return ProcessingResult.poisonPill(successfulIds, invalidTrade);
                    } catch (Exception ex) {
                        log.error("Failed to persist DLQ entry for invalid trade {}: {}",
                                invalidTrade.getInvalidTradeOutboxId(),
                                ex.getMessage());
                        return ProcessingResult.systemFailure(successfulIds);
                    }
                }

                return ProcessingResult.systemFailure(successfulIds);
            }
        }

        return ProcessingResult.success(successfulIds);
    }

    /**
     * Send DLQ event to RTTM when invalid trade processing fails
     */
    private void sendDlqEventToRttm(InvalidTradeEntity invalidTrade, String errorReason) {
        try {
            DlqEventPayload dlqEvent = DlqEventPayload.builder()
                    .tradeId(invalidTrade.getTradeId().toString())
                    .serviceName(serviceName)
                    .topicName("validation_invalid_trades_dlq") // DLQ topic for invalid trades
                    .originalTopic(invalidTradesTopic)
                    .reason(errorReason)
                    .eventStage(EventStage.VALIDATED)
                    .build();

            rttmClient.sendDlqEvent(dlqEvent);
            log.debug("Sent DLQ event to RTTM for invalid trade {}", invalidTrade.getTradeId());
        } catch (Exception ex) {
            log.warn("Failed to send DLQ event to RTTM: {}", ex.getMessage());
        }
    }

}
