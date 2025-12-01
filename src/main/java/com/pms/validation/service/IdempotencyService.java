package com.pms.validation.service;

import com.pms.validation.dao.ProcessedMessageRepository;
import com.pms.validation.entity.ProcessedMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.logging.Logger;

@Service
public class IdempotencyService {

    private static final Logger logger = Logger.getLogger(IdempotencyService.class.getName());
    private static final String CONSUMER_GROUP = "validation-consumer-group";

    @Autowired
    private ProcessedMessageRepository repository;

    @Transactional
    public boolean markAsProcessed(UUID eventId, String topic) {
        try {
            ProcessedMessage message = new ProcessedMessage(eventId, CONSUMER_GROUP, topic);
            repository.save(message);
            logger.info("Marked event " + eventId + " as processed");
            return true;
        } catch (Exception ex) {
            logger.warning("Event " + eventId + " already processed or constraint violation: " + ex.getMessage());
            return false;
        }
    }

    public boolean isAlreadyProcessed(UUID eventId) {
        return repository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP);
    }
}
