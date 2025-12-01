package com.pms.validation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.validation.dto.OutboxEventDto;
import com.pms.validation.dto.TradeDto;
import com.pms.validation.dto.ValidationEventDto;
import com.pms.validation.dto.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class KafkaConsumerService {

    private static final Logger logger = Logger.getLogger(KafkaConsumerService.class.getName());

    @Autowired
    private ValidationService validationService;

    @Autowired
    private ValidationOutboxService outboxService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private KafkaTemplate<String, ValidationEventDto> kafkaTemplate;

    @Autowired
    private ObjectMapper mapper;

    @KafkaListener(topics = "ingestion-topic", groupId = "validation-consumer-group")
    public void processIngestionMessage(String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        try {
            OutboxEventDto ingestionEvent = mapper.readValue(payload, OutboxEventDto.class);

            if (idempotencyService.isAlreadyProcessed(ingestionEvent.getEventId())) {
                logger.info("Ignoring duplicate event: " + ingestionEvent.getEventId());
                return;
            }

            TradeDto trade = mapper.readValue(ingestionEvent.getPayloadBytes(), TradeDto.class);

            logger.info("Processing trade from ingestion: " + trade.getTradeId());

            if (!idempotencyService.markAsProcessed(ingestionEvent.getEventId(), "ingestion-topic")) {
                logger.warning(
                        "Failed to mark event as processed (possible duplicate): " + ingestionEvent.getEventId());
                return;
            }

            ValidationResult result = validationService.validateOutbox(ingestionEvent);

            ValidationEventDto validationEvent = outboxService.buildValidationEvent(trade, result);

            String topic = result.isValid() ? "validation-topic" : "validation-dlq";
            outboxService.saveValidationEvent(trade, result, result.isValid() ? "SUCCESS" : "FAILED");

            kafkaTemplate.send(topic, validationEvent.getTradeId().toString(), validationEvent);
            logger.info("Sent validation event to " + topic + " for trade: " + trade.getTradeId());

        } catch (Exception ex) {
            logger.severe("Error processing ingestion message: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
