package com.pms.validation.service;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.proto.TradeEventProto;

@Service
public class KafkaConsumerService {

    private static final Logger logger = Logger.getLogger(KafkaConsumerService.class.getName());

    @Autowired
    private ValidationCore validationCore;

    @KafkaListener(topics = "ingestion-topic", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "jsonKafkaListenerContainerFactory")
    public void onIngestionMessage(TradeDto tradeDto,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        try {
            System.out.println("From Ingestion topic consumer service:");
            System.out.println("Received message from partition " + partition);
            System.out.println("Offset: " + offset);

            // TradeDto tradeDto = protoDTOMapper.toDto(tradeMessage);

            validationCore.processTrade(tradeDto);

        } catch (Exception ex) {
            logger.severe("Error in IngestionListener.onMessage: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @KafkaListener(topics = "validation-topic", groupId = "pms-core-consumer-group", containerFactory = "protobufKafkaListenerContainerFactory")
    public void onValidationMessage(TradeEventProto validatedTrade,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        try {
            System.out.println("From Validation topic consumer service:");
            System.out.println("Received message from partition " + partition);
            System.out.println("Offset: " + offset);

            // TradeEventProto validatedTrade = TradeEventProto.parseFrom(payload);

            System.out.println("Payload: " + validatedTrade);
        } catch (Exception ex) {
            logger.severe("Error in ValidationListener.onMessage: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @KafkaListener(topics = "invalid-trade-topic", groupId = "rttm-consumer-group", containerFactory = "protobufKafkaListenerContainerFactory")
    public void onInvalidTradeMessage(TradeEventProto invalidTrade,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        try {
            System.out.println("From Invalid Trade topic consumer service:");
            System.out.println("Received message from partition " + partition);
            System.out.println("Offset: " + offset);

            // TradeEventProto invalidTrade = TradeEventProto.parseFrom(payload);

            System.out.println("Payload: " + invalidTrade);
        } catch (Exception ex) {
            logger.severe("Error in ValidationListener.onMessage: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
