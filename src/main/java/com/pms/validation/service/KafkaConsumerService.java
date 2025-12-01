package com.pms.validation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.validation.dto.IngestionEventDto;
import com.pms.validation.dto.TradeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class KafkaConsumerService {

    private static final Logger logger = Logger.getLogger(KafkaConsumerService.class.getName());

    @Autowired
    private IngestionProcessor ingestionProcessor;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private ObjectMapper mapper;

    @KafkaListener(topics = "ingestion-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        try {
            IngestionEventDto ingestionEvent = mapper.readValue(payload, IngestionEventDto.class);

            // Deserialize payload to obtain tradeId for idempotency check
            TradeDto trade = mapper.readValue(ingestionEvent.getPayloadBytes(), TradeDto.class);

            // Fast check to avoid processing if trade already handled
            if (idempotencyService.isAlreadyProcessed(trade.getTradeId())) {
                logger.info("Ignoring duplicate trade: " + trade.getTradeId());
                return;
            }

            logger.info("Delegating trade " + trade.getTradeId() + " to processor");

            ingestionProcessor.process(ingestionEvent, trade);

        } catch (Exception ex) {
            logger.severe("Error in IngestionListener.onMessage: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
