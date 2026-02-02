package com.pms.validation.event;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import java.util.concurrent.LinkedBlockingDeque;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;

import com.pms.validation.proto.TradeEventProto;
import com.pms.validation.service.health.DbHealthMonitor;
import com.pms.validation.wrapper.PollBatch;
import com.pms.validation.service.BatchProcessor;
import com.pms.rttm.client.clients.RttmClient;
import com.pms.rttm.client.dto.DlqEventPayload;
import com.pms.rttm.client.enums.EventStage;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KafkaConsumerService {

    @Autowired
    private LinkedBlockingDeque<PollBatch> buffer;

    @Autowired
    private BatchProcessor batchProcessor;

    

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
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets,
            @Header(KafkaHeaders.GROUP_ID) String consumerGroup) {
        try {
            log.info("Received {} trade messages from partition {}", messages.size(), partition);

            // Enqueue the poll into the local bounded buffer for coalesced batching
            buffer.offer(new PollBatch(messages, ack));

            int totalMessagesInBuffer = buffer.stream()
        .mapToInt(poll -> poll.getTradeProtos().size())
        .sum();

log.info("Enqueued {} trades from partition {}. Total messages in buffer now: {}",
        messages.size(), partition, totalMessagesInBuffer);

            // If buffer is growing large, instruct batch processor to pause consumer
            if (buffer.size() >= 40) {
                batchProcessor.handleConsumerThread(false);
            }

            // Trigger a flush if thresholds are met (async)
            batchProcessor.checkAndFlush();

        } catch (CannotCreateTransactionException | DataAccessException ex) {
            log.error("DB Down -->  pausing Kafka consumer", ex);
            // Pause the consumer on DB failures or connectivity issues
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
