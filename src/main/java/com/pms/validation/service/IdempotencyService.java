package com.pms.validation.service;

import com.pms.validation.dao.ProcessedMessageRepository;
import com.pms.validation.entity.ProcessedMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.logging.Logger;

@Service
public class IdempotencyService {

    private static final Logger logger = Logger.getLogger(IdempotencyService.class.getName());

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @Autowired
    private ProcessedMessageRepository repository;

    @Transactional
    public boolean markAsProcessed(UUID tradeId, String topic) {
        try {
            ProcessedMessage message = new ProcessedMessage(tradeId, consumerGroup, topic);
            repository.save(message);
            logger.info("Marked trade " + tradeId + " as processed");
            return true;
        } catch (Exception ex) {
            logger.warning("Trade " + tradeId + " already processed or constraint violation: " + ex.getMessage());
            return false;
        }
    }

    public boolean isAlreadyProcessed(UUID tradeId) {
        return repository.existsByTradeIdAndConsumerGroup(tradeId, consumerGroup);
    }
}
