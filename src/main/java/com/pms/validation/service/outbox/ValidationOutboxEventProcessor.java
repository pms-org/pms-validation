package com.pms.validation.service.outbox;

import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.common.errors.SerializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.proto.TradeEventProto;
import com.pms.validation.mapper.ProtoEntityMapper;
import com.pms.validation.repository.ValidationOutboxRepository;
import com.pms.validation.repository.DlqRepository;
import com.pms.validation.entity.DlqEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.pms.rttm.client.clients.RttmClient;
import com.pms.rttm.client.dto.TradeEventPayload;
import com.pms.rttm.client.dto.DlqEventPayload;
import com.pms.rttm.client.enums.EventType;
import com.pms.rttm.client.enums.EventStage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationOutboxEventProcessor {

    private final ValidationOutboxRepository outboxRepo;

    private final AdaptiveBatchSizer batchSizer;

    private final KafkaTemplate<String, TradeEventProto> kafkaTemplate;

    private final DlqRepository dlqRepository;

    private final RttmClient rttmClient;

    @Value("${app.outgoing-valid-trades-topic}")
    private String validTradesTopic;

    @Value("${app.incoming-trades-topic}")
    private String incomingTradesTopic;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @Value("${spring.application.name}")
    private String serviceName;

    @Transactional
    public ProcessingResult<ValidationOutboxEntity> dispatchOnce() {

        int limit = batchSizer.getCurrentSize();
        log.info("Limit of this batch {}.", limit);

        List<ValidationOutboxEntity> batch = outboxRepo.findPendingWithPortfolioXactLock(limit);

        log.info("Fetched {} from validation_outbox.", batch.size());

        if (batch.isEmpty()) {
            batchSizer.reset();
            return ProcessingResult.success(List.of());
        }

        long start = System.currentTimeMillis();

        ProcessingResult<ValidationOutboxEntity> result = process(batch);

        long duration = System.currentTimeMillis() - start;

        if (!result.systemFailure()) {
            batchSizer.adjust(duration, batch.size());
        }

        if (!result.successfulIds().isEmpty()) {
            outboxRepo.markAsSent(result.successfulIds());
            log.info("Updated {} outbox events to SENT", result.successfulIds());
        }

        if (result.poisonPill() != null) {
            ValidationOutboxEntity poison = result.poisonPill();
            outboxRepo.markAsFailed(poison.getValidationOutboxId());
        }

        return result;
    }

    public ProcessingResult<ValidationOutboxEntity> process(List<ValidationOutboxEntity> events) {

        List<Long> successfulIds = new ArrayList<>();

        for (ValidationOutboxEntity outbox : events) {
            try {
                TradeEventProto proto = ProtoEntityMapper.toProto(outbox);

                var sendResult = kafkaTemplate.send(validTradesTopic, proto.getPortfolioId(), proto).get();
                var metadata = sendResult.getRecordMetadata();
                int partition = metadata.partition();
                long offset = metadata.offset();

                log.info("Valid Trade with ID {} sent to kafka successfully.", proto.getTradeId());

                // Don't Send trade completion event to RTTM again for same trade id
                // Send trade completion event to RTTM
                // sendTradeCompletionEvent(outbox, partition, offset);

                successfulIds.add(outbox.getValidationOutboxId());

            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                log.error("Error sending outbox {} : {}", outbox.getValidationOutboxId(), e.getMessage());

                // Simple poison-pill classification: serialization and illegal argument are
                // considered poison
                boolean poison = cause instanceof SerializationException
                        || cause instanceof IllegalArgumentException
                        || cause instanceof JsonProcessingException;

                if (poison) {
                    try {
                        // attempt to persist payload to DLQ for later inspection
                        TradeEventProto proto = null;
                        try {
                            proto = ProtoEntityMapper.toProto(outbox);
                        } catch (Exception ex) {
                            log.warn("Failed to convert outbox {} to proto for DLQ persistence: {}", 
                                    outbox.getValidationOutboxId(), ex.getMessage());
                            // We'll persist empty payload if proto can't be built
                        }

                        byte[] payload = proto != null ? proto.toByteArray() : new byte[0];

                        DlqEntry entry = DlqEntry.builder()
                                .payload(payload)
                                .errorDetail(e.toString())
                                .build();

                        dlqRepository.save(entry);
                        log.warn("Persisted outbox {} to DLQ as id {}", outbox.getValidationOutboxId(),
                                entry.getDlqEntryid());

                        // Send DLQ event to RTTM
                        sendDlqEventToRttm(outbox, e.getMessage());

                        // mark as failed so dispatcher won't retry endlessly
                        return ProcessingResult.poisonPill(successfulIds, outbox);
                    } catch (Exception ex) {
                        log.error("Failed to persist DLQ entry for outbox {}: {}", outbox.getValidationOutboxId(),
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
     * Send trade completion event to RTTM
     */
    private void sendTradeCompletionEvent(ValidationOutboxEntity outbox, int partition, long offset) {
        try {
            TradeEventPayload event = TradeEventPayload.builder()
                    .tradeId(outbox.getTradeId().toString())
                    .serviceName(serviceName)
                    .eventType(EventType.TRADE_VALIDATED)
                    .eventStage(EventStage.VALIDATED)
                    .eventStatus("OK")
                    .sourceQueue(incomingTradesTopic)
                    .targetQueue(validTradesTopic)
                    .message("Trade dispatched to downstream service")
                    .topicName(validTradesTopic)
                    .offsetValue(offset)
                    .partitionId(partition)
                    .consumerGroup(consumerGroup)
                    .build();

            rttmClient.sendTradeEvent(event);
            log.debug("Sent trade completion event to RTTM for trade {}", outbox.getTradeId());
        } catch (Exception ex) {
            log.warn("Failed to send trade completion event to RTTM: {}", ex.getMessage());
        }
    }

    /**
     * Send DLQ event to RTTM when outbox processing fails
     */
    private void sendDlqEventToRttm(ValidationOutboxEntity outbox, String errorReason) {
        try {
            DlqEventPayload dlqEvent = DlqEventPayload.builder()
                    .tradeId(outbox.getTradeId().toString())
                    .serviceName(serviceName)
                    .topicName("validation_outbox") // change our own DLT Topic if needed which is not present
                    .originalTopic(validTradesTopic)
                    .reason(errorReason)
                    .eventStage(EventStage.VALIDATED)
                    .build();

            rttmClient.sendDlqEvent(dlqEvent);
            log.debug("Sent DLQ event to RTTM for trade {}", outbox.getTradeId());
        } catch (Exception ex) {
            log.warn("Failed to send DLQ event to RTTM: {}", ex.getMessage());
        }
    }

}
