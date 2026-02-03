package com.pms.validation.event;

import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.pms.validation.proto.TradeEventProto;
import com.pms.validation.service.processing.ValidationBatchProcessor;
import com.pms.validation.wrapper.PollBatch;
import com.pms.rttm.client.clients.RttmClient;
import com.pms.rttm.client.dto.DlqEventPayload;
import com.pms.rttm.client.enums.EventStage;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KafkaConsumerService {

    @Autowired
    private LinkedBlockingDeque<PollBatch> validationBuffer;

    @Autowired
    private ValidationBatchProcessor batchProcessor;

    @Autowired
    private RttmClient rttmClient;

    @Value("${spring.application.name}")
    private String serviceName;
    
    @Value("${app.buffer.size:50}")
    private int bufferSize;

    // Batch consumer: receives a list of protobuf messages and manual ack
    @KafkaListener(id = "tradesListener", topics = "${app.incoming-trades-topic}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "protobufKafkaListenerContainerFactory")
    public void consume(List<TradeEventProto> messages, Acknowledgment ack,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.RECEIVED_TOPIC) List<String> topics,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets,
            @Header(KafkaHeaders.GROUP_ID) String consumerGroup) {

        log.info("Received {} trade messages from partition {}", messages.size(), partition);

        // Extract single topic name from list (all messages in batch should be from same topic)
        String topic = (topics != null && !topics.isEmpty()) ? topics.get(0) : "unknown-topic";

        // Create PollBatch and offer to buffer
        PollBatch pollBatch = new PollBatch(messages, ack, partition, topic, offsets, consumerGroup);
        validationBuffer.offer(pollBatch);
        
        log.debug("Added batch to buffer. Current buffer size: {}", validationBuffer.size());

        // Check if we should trigger batch processing
        if (validationBuffer.size() >= bufferSize * 0.8) {
            batchProcessor.checkAndFlush();
        }
        
        batchProcessor.checkAndFlush();
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
