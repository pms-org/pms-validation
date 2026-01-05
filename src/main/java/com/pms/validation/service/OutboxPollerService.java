package com.pms.validation.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.repository.ValidationOutboxRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OutboxPollerService {

    @Autowired
    private ValidationOutboxRepository outboxRepo;

    @Autowired
    private OutboxProcessorService outboxProcessorService;

    private static final int BATCH_SIZE = 50;

    @Scheduled(fixedDelay = 2000)
    public void pollAndPublish() {

        List<ValidationOutboxEntity> pending = outboxRepo.findBySentStatusOrderByValidationOutboxIdAsc("PENDING",
                PageRequest.of(0, BATCH_SIZE));

        if (pending.isEmpty()) {
            return;
        }

        for (ValidationOutboxEntity outbox : pending) {
            try {

                outboxProcessorService.processValidOutbox(outbox.getValidationOutboxId());

            } catch (Exception ex) {
                log.error("Failed publishing outbox {}: {}",
                        outbox.getValidationOutboxId(), ex.getMessage());
            }
        }
    }
}
