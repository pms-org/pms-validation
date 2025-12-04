package com.pms.validation.service;

import java.time.LocalDateTime;
import java.util.List;

import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.pms.validation.entity.InvalidTradeEntity;
import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.repository.InvalidTradeRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InvalidTradePollerService {

    @Autowired
    private InvalidTradeRepository invalidOutboxRepo;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void pollAndPublish() {

        List<InvalidTradeEntity> pending = invalidOutboxRepo
                .fetchPending(Sort.by(Sort.Direction.ASC, "invalidTradeId"));

        if (pending.isEmpty())
            return;

        for (InvalidTradeEntity outbox : pending) {
            try {
                log.info("Publishing invalid trade outbox record {} for trade {}",
                        outbox.getInvalidTradeId() , outbox.getTradeId());

                kafkaProducerService.sendInvalidTradeEvent(outbox);

                outbox.setSentStatus("SENT");
                outbox.setUpdatedAt(LocalDateTime.now());
                invalidOutboxRepo.save(outbox);

            } catch (Exception ex) {
                log.error("Failed publishing outbox {}: {}",
                        outbox.getInvalidTradeId(), ex.getMessage());

                outbox.setSentStatus("FAILED");
                outbox.setUpdatedAt(LocalDateTime.now());
                invalidOutboxRepo.save(outbox);
            }
        }
    }
}
