package com.pms.validation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.pms.validation.entity.ValidationOutbox;



@Service
public class KafkaConsumerService {

    @Autowired
    private ValidationOutboxService validationOutboxService;

    @KafkaListener(topics = "ingestion-topic",groupId = "ingestion-consumer-group")
    public void processIngestionMessage(ValidationOutbox ingestionTrade,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) Long offset 
    ) {
        System.out.println("Received from partition: " + partition);
        System.out.println("Offset: " + offset);

        System.out.println("Consumed trade from Kafka: " + ingestionTrade);

        validationOutboxService.validateTrade(ingestionTrade);
    }

    @KafkaListener(topics = "validation-topic",groupId = "validation-consumer-group")
    public void processValidationMessage(ValidationOutbox validatedTrade,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) Long offset 
    ) {
        System.out.println("Received from partition: " + partition);
        System.out.println("Offset: " + offset);
        System.out.println("Consumed trade from Kafka: " + validatedTrade);
    }


}
