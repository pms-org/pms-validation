package com.pms.validation.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.event.KafkaProducerService;
import com.pms.validation.repository.ValidationOutboxRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OutboxPollerService {

    @Autowired
    private ValidationOutboxRepository outboxRepo;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Scheduled(fixedDelay = 2000)
    public void pollAndPublish() {

        List<ValidationOutboxEntity> pending = outboxRepo
                .fetchPending(Sort.by(Sort.Direction.ASC, "validationOutboxId"));

        if (pending.isEmpty())
            return;

        for (ValidationOutboxEntity outbox : pending) {
            try {
                log.info("Publishing outbox record {} for trade {}",
                        outbox.getValidationOutboxId(), outbox.getTradeId());

                kafkaProducerService.sendValidationEvent(outbox);

                outbox.setSentStatus("SENT");
                outbox.setUpdatedAt(LocalDateTime.now());
                outboxRepo.save(outbox);

            } catch (Exception ex) {
                log.error("Failed publishing outbox {}: {}",
                        outbox.getValidationOutboxId(), ex.getMessage());

                outbox.setSentStatus("FAILED");
                outbox.setUpdatedAt(LocalDateTime.now());
                outboxRepo.save(outbox);
            }
        }
    }
}
