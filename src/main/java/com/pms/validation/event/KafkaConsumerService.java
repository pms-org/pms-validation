package com.pms.validation.event;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
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

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    // Batch consumer: receives ConsumerRecords which reliably contain partition
    // info
    @KafkaListener(id = "tradesListener", topics = "${app.incoming-trades-topic}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "protobufKafkaListenerContainerFactory")
    public void consume(List<ConsumerRecord<String, TradeEventProto>> records, Acknowledgment ack) {

        log.info("Received {} trade messages", records.size());

        // Extract messages, partitions, offsets, and topic from ConsumerRecords
        List<TradeEventProto> messages = new ArrayList<>();
        List<Integer> partitions = new ArrayList<>();
        List<Long> offsets = new ArrayList<>();
        String topic = null;

        for (ConsumerRecord<String, TradeEventProto> record : records) {
            messages.add(record.value());
            partitions.add(record.partition());
            offsets.add(record.offset());
            if (topic == null) {
                topic = record.topic();
            }
        }

        log.debug("Messages from topic {} with partitions {}", topic, partitions);

        // Create PollBatch and offer to buffer
        PollBatch pollBatch = new PollBatch(messages, ack, partitions, topic, offsets, consumerGroupId);
        validationBuffer.offer(pollBatch);

        log.debug("Added batch to buffer. Current buffer size: {}", validationBuffer.size());

        // Check if we should trigger batch processing
        if (validationBuffer.size() >= bufferSize * 0.8) {
            batchProcessor.checkAndFlush();
        }

        // batchProcessor.checkAndFlush();
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
                    .eventStage(EventStage.RECEIVED)
                    .build();

            rttmClient.sendDlqEvent(dlqEvent);
            log.info("Sent DLQ event to RTTM for trade {}", dltMessage.getTradeId());
        } catch (Exception ex) {
            log.warn("Failed to send DLQ event to RTTM: {}", ex.getMessage());
        }
    }

}
