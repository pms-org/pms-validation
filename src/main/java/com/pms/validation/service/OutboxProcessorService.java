package com.pms.validation.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.validation.entity.InvalidTradeEntity;
import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.event.KafkaProducerService;
import com.pms.validation.repository.InvalidTradeRepository;
import com.pms.validation.repository.ValidationOutboxRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OutboxProcessorService {

    @Autowired
    private ValidationOutboxRepository outboxRepo;

    @Autowired
    private InvalidTradeRepository invalidOutboxRepo;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Transactional
    public void processValidOutbox(long id) {

        ValidationOutboxEntity lockedOutbox = outboxRepo.lockByValidationOutboxId(id);

        if (lockedOutbox.getSentStatus().equals("SENT"))
            return;

        try {

            kafkaProducerService.sendValidationEvent(lockedOutbox);

            lockedOutbox.setSentStatus("SENT");
            lockedOutbox.setUpdatedAt(LocalDateTime.now());
            outboxRepo.save(lockedOutbox);

        } catch (Exception e) {

            log.error("Failed to send outbox record {} {}", id, e.getMessage());

            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void processInvalidOutbox(long id) {

        InvalidTradeEntity lockedOutbox = invalidOutboxRepo.lockByInvalidTradeOutboxId(id);

        if (!lockedOutbox.getSentStatus().equals("SENT")) {
        } else {
            return;
        }

        try {

            kafkaProducerService.sendInvalidTradeEvent(lockedOutbox);

            lockedOutbox.setSentStatus("SENT");
            lockedOutbox.setUpdatedAt(LocalDateTime.now());
            invalidOutboxRepo.save(lockedOutbox);

        } catch (Exception e) {

            log.error("Failed to send outbox record {} {}", id, e.getMessage());

            throw new RuntimeException(e);
        }
    }

}
