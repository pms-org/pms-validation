package com.pms.validation.event;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.mapper.ProtoDTOMapper;
import com.pms.validation.proto.TradeEventProto;
import com.pms.validation.service.domain.TradeIdempotencyService;
import com.pms.validation.service.health.DbHealthMonitor;
import com.pms.validation.service.processing.ValidationBatchProcessingService;
import com.pms.rttm.client.clients.RttmClient;
import com.pms.rttm.client.dto.DlqEventPayload;
import com.pms.rttm.client.enums.EventStage;

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
    private DbHealthMonitor dbHealthMonitor;

    @Autowired
    private RttmClient rttmClient;

    @Value("${spring.application.name}")
    private String serviceName;

    // Batch consumer: receives a list of protobuf messages and manual ack
    @KafkaListener(id = "tradesListener", topics = "${app.incoming-trades-topic}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "protobufKafkaListenerContainerFactory")
    public void consume(List<TradeEventProto> messages, Acknowledgment ack,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.RECEIVED_TOPIC) List<String> topics,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets,
            @Header(KafkaHeaders.GROUP_ID) String consumerGroup) {

        try {
            log.info("Received {} trade messages from partition {}", messages.size(), partition);

            // Extract single topic name from list (all messages in batch should be from
            // same topic)
            String topic = (topics != null && !topics.isEmpty()) ? topics.get(0) : "unknown-topic";

            // Convert to DTOs and delegate to batch processor
            batchProcessingService.processBatch(messages, partition, topic, offsets, consumerGroup);

            ack.acknowledge();
        } catch (CannotCreateTransactionException | DataAccessException ex) {
            log.error("DB Down -->  pausing Kafka consumer", ex);
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
        log.error("Trade moved to DLT | DLT Topic={} DLT message={} topic={} partition={} offset={}", dltTopic,
                dltMessage, originalTopic, partition, offset);

        // Send DLQ event to RTTM
        try {
            DlqEventPayload dlqEvent = DlqEventPayload.builder()
                    .tradeId(dltMessage.getTradeId())
                    .serviceName(serviceName)
                    .topicName(dltTopic)
                    .originalTopic(originalTopic)
                    .reason("Deserialization error or max retries exceeded")
                    .eventStage(EventStage.CONSUME)
                    .build();

            rttmClient.sendDlqEvent(dlqEvent);
            log.info("Sent DLQ event to RTTM for trade {}", dltMessage.getTradeId());
        } catch (Exception ex) {
            log.warn("Failed to send DLQ event to RTTM: {}", ex.getMessage());
        }
    }

}
