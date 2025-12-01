package com.pms.validation.service;

import com.pms.validation.dto.ValidationOutputDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, ValidationOutputDto> kafkaTemplate;

    public void sendValidationEvent(String topic, ValidationOutputDto event) {
        kafkaTemplate.send(topic, event.getTradeId().toString(), event);
    }
}
