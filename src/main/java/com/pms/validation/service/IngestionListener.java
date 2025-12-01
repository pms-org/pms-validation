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
public class IngestionListener {

    private static final Logger logger = Logger.getLogger(IngestionListener.class.getName());

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

            // Fast check to avoid deserialization/processing if already handled
            if (idempotencyService.isAlreadyProcessed(ingestionEvent.getEventId())) {
                logger.info("Ignoring duplicate event: " + ingestionEvent.getEventId());
                return;
            }

            TradeDto trade = mapper.readValue(ingestionEvent.getPayloadBytes(), TradeDto.class);

            logger.info("Delegating trade " + trade.getTradeId() + " to processor");

            ingestionProcessor.process(ingestionEvent, trade);

        } catch (Exception ex) {
            logger.severe("Error in IngestionListener.onMessage: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
