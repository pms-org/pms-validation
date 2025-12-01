package com.pms.validation.service;

import com.pms.validation.entity.ValidationOutboxEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, ValidationOutboxEntity> kafkaTemplate;

    private static final String topic = "validation-topic";

    public void sendValidationEvent(ValidationOutboxEntity event) {
        kafkaTemplate.send(topic, event.getPortfolioId().toString(), event);
    }
}
