package com.pms.validation.event;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.exception.RetryableException;
import com.pms.validation.mapper.ProtoDTOMapper;
import com.pms.validation.proto.TradeEventProto;
import com.pms.validation.service.TradeProcessingService;
import com.pms.validation.service.ValidationCore;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KafkaConsumerService {

    private static final Logger logger = Logger.getLogger(KafkaConsumerService.class.getName());

    @Autowired
    private TradeProcessingService tradeProcessingService;

    @RetryableTopic(attempts = "5", include = {
            RetryableException.class }, backoff = @Backoff(delay = 2000, multiplier = 2))
    @KafkaListener(topics = "${app.incoming-topic}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "protobufKafkaListenerContainerFactory")
    public void onIngestionMessage(TradeEventProto tradeMessage,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) Long offset) {

        TradeDto tradeDto = ProtoDTOMapper.toDto(tradeMessage);

        tradeProcessingService.processTrade(tradeDto);

        log.info("Successfully processed trade {} from ingestion topic", tradeDto.getTradeId());

    }

    @KafkaListener(topics = "${app.outgoing-validated-topic}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "protobufKafkaListenerContainerFactory")
    public void onValidationMessage(TradeEventProto validatedTrade,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        try {
            System.out.println("From Validation topic consumer service:");
            System.out.println("Received message from partition " + partition);
            System.out.println("Offset: " + offset);

            System.out.println("Payload: " + validatedTrade);
        } catch (Exception ex) {
            logger.severe("Error in ValidationListener.onMessage: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @KafkaListener(topics = "${app.outgoing-invalidated-topic}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "protobufKafkaListenerContainerFactory")
    public void onInvalidTradeMessage(TradeEventProto invalidTrade,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        try {
            System.out.println("From Invalid Trade topic consumer service:");
            System.out.println("Received message from partition " + partition);
            System.out.println("Offset: " + offset);

            System.out.println("Payload: " + invalidTrade);
        } catch (Exception ex) {
            logger.severe("Error in ValidationListener.onMessage: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @DltHandler
    public void handleDltMessage(
            TradeEventProto dltMessage,
            @Header(KafkaHeaders.ORIGINAL_TOPIC) String originalTopic,
            @Header(KafkaHeaders.ORIGINAL_PARTITION) int partition,
            @Header(KafkaHeaders.ORIGINAL_OFFSET) long offset) {
        System.out.println("DLT MESSAGE RECEIVED");
        System.out.println("Payload: " + dltMessage);
        System.out.println("From Topic: " + originalTopic);
        System.out.println("Partition: " + partition);
        System.out.println("Offset: " + offset);
    }
}
