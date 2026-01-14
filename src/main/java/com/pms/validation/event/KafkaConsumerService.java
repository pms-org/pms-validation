package com.pms.validation.event;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.mapper.ProtoDTOMapper;
import com.pms.validation.proto.TradeEventProto;
import com.pms.validation.service.TradeIdempotencyService;
import com.pms.validation.service.TradeProcessingService;
import com.pms.validation.service.ValidationBatchProcessingService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KafkaConsumerService {

    @Autowired
    private ValidationBatchProcessingService batchProcessingService;

    @Autowired
    private TradeIdempotencyService tradeIdempotencyService;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private com.pms.validation.service.DbHealthMonitor dbHealthMonitor;

    // Batch consumer: receives a list of protobuf messages and manual ack
    @KafkaListener(id = "tradesListener", topics = "${app.incoming-trades-topic}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "protobufKafkaListenerContainerFactory")
    public void consume(List<TradeEventProto> messages, Acknowledgment ack, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {

        try {
            log.info("Received {} trade messages from partition {}", messages.size(), partition);

            // Convert to DTOs and delegate to batch processor
            batchProcessingService.processBatch(messages);

            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Batch processing failed, pausing consumer", ex);
            // Pause the consumer on DB failures or connectivity issues
            // Stop the listener and start monitoring to resume when DB is back
            var container = registry.getListenerContainer("tradesListener");
            if (container != null) {
                container.stop();
            }
            dbHealthMonitor.pause();
        }
    }

    @DltHandler
    public void handleDltMessage(
            TradeEventProto dltMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String dltTopic,
            @Header(KafkaHeaders.ORIGINAL_TOPIC) String originalTopic,
            @Header(KafkaHeaders.ORIGINAL_PARTITION) int partition,
            @Header(KafkaHeaders.ORIGINAL_OFFSET) long offset) {
        // TODO: send DLT msg to rttm
        log.error("Trade moved to DLT | DLT Topic={} DLT message={} topic={} partition={} offset={}", dltTopic,
                dltMessage, originalTopic, partition, offset);
    }

}
