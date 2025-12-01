package com.pms.validation.service;

import com.pms.validation.dto.IngestionEventDto;
import com.pms.validation.dto.TradeDto;
import com.pms.validation.dto.ValidationOutputDto;
import com.pms.validation.dto.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class IngestionProcessor {

    private static final Logger logger = Logger.getLogger(IngestionProcessor.class.getName());

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private TradeValidationService tradeValidationService;

    @Autowired
    private ValidationOutboxService validationOutboxService;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    public void process(IngestionEventDto ingestionEvent, TradeDto trade) {
        try {
            // Atomic mark - prevents races
            boolean marked = idempotencyService.markAsProcessed(ingestionEvent.getEventId(), "ingestion-topic");
            if (!marked) {
                logger.info("Event already processed (during mark): " + ingestionEvent.getEventId());
                return;
            }

            // Validate
            ValidationResult result = tradeValidationService.validateTrade(trade);

            // Build event and persist outbox
            ValidationOutputDto output = validationOutboxService.buildValidationEvent(trade, result);
            String status = result.isValid() ? "SUCCESS" : "FAILED";
            validationOutboxService.saveValidationEvent(trade, result, status);

            // Publish
            String topic = result.isValid() ? "validation-topic" : "validation-dlq";
            kafkaProducerService.sendValidationEvent(topic, output);
            logger.info("Published validation event for trade " + trade.getTradeId() + " to " + topic);

        } catch (Exception ex) {
            logger.severe("Error in IngestionProcessor.process: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
