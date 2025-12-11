package com.pms.validation.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.pms.validation.entity.InvalidTradeEntity;
import com.pms.validation.repository.InvalidTradeRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InvalidTradePollerService {

    @Autowired
    private InvalidTradeRepository invalidOutboxRepo;

    @Autowired
    private OutboxProcessorService outboxProcessorService;

    @Scheduled(fixedDelay = 2000)
    public void pollAndPublish() {

        List<InvalidTradeEntity> pending = invalidOutboxRepo
                .findBySentStatusOrderByInvalidTradeOutboxIdAsc("PENDING",
                       PageRequest.of(0, 50));

        if (pending.isEmpty())
            return;

        for (InvalidTradeEntity outbox : pending) {
            try {
                
                outboxProcessorService.processInvalidOutbox(outbox.getInvalidTradeOutboxId());

            } catch (Exception ex) {
                log.error("Failed publishing outbox {}: {}",
                        outbox.getInvalidTradeOutboxId(), ex.getMessage());
            }
        }
    }
}
