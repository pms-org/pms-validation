package com.pms.validation.service.metrics;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.pms.rttm.client.clients.RttmClient;
import com.pms.rttm.client.dto.QueueMetricPayload;

import lombok.extern.slf4j.Slf4j;

/**
 * Service to send queue metrics to RTTM at regular intervals.
 * This tracks the offset positions of the validation service's input/output
 * queues.
 */
@Service
@Slf4j
public class QueueMetricsService {

    @Autowired
    private RttmClient rttmClient;

    @Value("${app.incoming-trades-topic:pms.validation.in}")
    private String incomingTopic;

    @Value("${app.outgoing-valid-trades-topic:pms.validation.out.valid}")
    private String validTradesTopic;

    @Value("${app.outgoing-invalid-trades-topic:pms.validation.out.invalid}")
    private String invalidTradesTopic;

    @Value("${spring.kafka.consumer.group-id:pms-validation-cg}")
    private String consumerGroup;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.application.name}")
    private String serviceName;

    /**
     * Send queue metrics every 30 seconds
     */
    @Scheduled(fixedDelayString = "${rttm.metrics.interval-ms:30000}")
    public void sendQueueMetrics() {
        try (KafkaConsumer<String, String> consumer = createKafkaConsumer()) {
            // Send metrics for incoming topic
            sendMetricsForAllPartitions(consumer, incomingTopic);

            // Send metrics for valid trades topic
            sendMetricsForAllPartitions(consumer, validTradesTopic);

            // Send metrics for invalid trades topic
            sendMetricsForAllPartitions(consumer, invalidTradesTopic);

            log.debug("Queue metrics sent to RTTM successfully");
        } catch (Exception ex) {
            log.warn("Failed to send queue metrics to RTTM: {}", ex.getMessage());
        }
    }

    /**
     * Send metrics for all partitions of a topic
     */
    private void sendMetricsForAllPartitions(KafkaConsumer<String, String> consumer, String topicName) {
        try {
            // Discover all partitions for this topic
            List<PartitionInfo> partitionInfos = consumer.partitionsFor(topicName);

            if (partitionInfos == null || partitionInfos.isEmpty()) {
                log.warn("No partitions found for topic: {}", topicName);
                return;
            }

            // Send metric for each partition
            for (PartitionInfo partitionInfo : partitionInfos) {
                sendMetricForTopic(consumer, topicName, partitionInfo.partition());
            }
        } catch (Exception ex) {
            log.warn("Failed to get partitions for topic {}: {}", topicName, ex.getMessage());
        }
    }

    /**
     * Send queue metric for a specific topic partition
     */
    private void sendMetricForTopic(KafkaConsumer<String, String> consumer, String topicName, int partitionId) {
        try {
            TopicPartition topicPartition = new TopicPartition(topicName, partitionId);

            // Get the end offset (latest produced offset)
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Collections.singletonList(topicPartition));
            long producedOffset = endOffsets.getOrDefault(topicPartition, 0L);

            // Get the committed offset (consumed offset) for this consumer group
            OffsetAndMetadata committedOffset = consumer.committed(Collections.singleton(topicPartition), Duration.ofSeconds(5)).get(topicPartition);
            long consumedOffset = (committedOffset != null) ? committedOffset.offset() : 0L;

            QueueMetricPayload metric = QueueMetricPayload.builder()
                    .serviceName(serviceName)
                    .topicName(topicName)
                    .partitionId(partitionId)
                    .producedOffset(producedOffset)
                    .consumedOffset(consumedOffset)
                    .consumerGroup(consumerGroup)
                    .build();
            rttmClient.sendQueueMetric(metric);
            log.debug("Sent queue metric for topic {} partition {} - produced: {}, consumed: {}",
                    topicName, partitionId, producedOffset, consumedOffset);
        } catch (Exception ex) {
            log.warn("Failed to send queue metric for {}: {}", topicName, ex.getMessage());
        }
    }

    /**
     * Create a temporary Kafka consumer to query offsets
     */
    private KafkaConsumer<String, String> createKafkaConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        return new KafkaConsumer<>(props);
    }
}
