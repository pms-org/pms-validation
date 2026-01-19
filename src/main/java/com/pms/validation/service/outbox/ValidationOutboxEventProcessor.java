package com.pms.validation.service.outbox;

import java.util.ArrayList;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.proto.TradeEventProto;
import com.pms.validation.mapper.ProtoEntityMapper;
import com.pms.validation.repository.ValidationOutboxRepository;
import com.pms.validation.repository.DlqRepository;
import com.pms.validation.entity.DlqEntry;

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

    private static final String TOPIC = "portfolio-risk-metrics"; // adjust as needed

    @Transactional
    public ProcessingResult dispatchOnce() {

        int limit = batchSizer.getCurrentSize();
        log.info("Limit of this batch {}.", limit);

        List<ValidationOutboxEntity> batch = outboxRepo.findPendingWithPortfolioXactLock(limit);

        log.info("Fetched {} from validation_outbox.", batch.size());

        if (batch.isEmpty()) {
            batchSizer.reset();
            return ProcessingResult.success(List.of());
        }

        long start = System.currentTimeMillis();

        ProcessingResult result = process(batch);

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

    public ProcessingResult process(List<ValidationOutboxEntity> events) {

        List<Long> successfulIds = new ArrayList<>();

        for (ValidationOutboxEntity outbox : events) {
            try {
                TradeEventProto proto = ProtoEntityMapper.toProto(outbox);

                kafkaTemplate.send(TOPIC, proto.getPortfolioId(), proto).get();

                log.info("Event {} sent to kafka successfully.", proto);

                successfulIds.add(outbox.getValidationOutboxId());

            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                log.error("Error sending outbox {} : {}", outbox.getValidationOutboxId(), e.getMessage());

                // Simple poison-pill classification: serialization and illegal argument are considered poison
                boolean poison = cause instanceof org.apache.kafka.common.errors.SerializationException
                        || cause instanceof IllegalArgumentException
                        || cause instanceof com.fasterxml.jackson.core.JsonProcessingException;

                if (poison) {
                    try {
                        // attempt to persist payload to DLQ for later inspection
                        TradeEventProto proto = null;
                        try {
                            proto = ProtoEntityMapper.toProto(outbox);
                        } catch (Exception ex) {
                            // ignore, we'll persist empty payload if proto can't be built
                        }

                        byte[] payload = proto != null ? proto.toByteArray() : new byte[0];

                        DlqEntry entry = DlqEntry.builder()
                                .payload(payload)
                                .errorDetail(e.toString())
                                .build();

                        dlqRepository.save(entry);
                        log.warn("Persisted outbox {} to DLQ as id {}", outbox.getValidationOutboxId(), entry.getId());
                        // mark as failed so dispatcher won't retry endlessly
                        return ProcessingResult.poisonPill(successfulIds, outbox);
                    } catch (Exception ex) {
                        log.error("Failed to persist DLQ entry for outbox {}: {}", outbox.getValidationOutboxId(), ex.getMessage());
                        return ProcessingResult.systemFailure(successfulIds);
                    }
                }

                return ProcessingResult.systemFailure(successfulIds);
            }
        }

        return ProcessingResult.success(successfulIds);
    }

}
