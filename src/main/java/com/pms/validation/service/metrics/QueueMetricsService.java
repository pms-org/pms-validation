package com.pms.validation.service.metrics;

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

    /**
     * Send queue metrics every 30 seconds
     * In a production system, you would fetch actual offset values from Kafka
     */
    @Scheduled(fixedDelayString = "${rttm.metrics.interval-ms:30000}")
    public void sendQueueMetrics() {
        try {
            // Send metrics for incoming topic
            sendMetricForTopic(incomingTopic, 0);

            // Send metrics for valid trades topic
            sendMetricForTopic(validTradesTopic, 1);

            // Send metrics for invalid trades topic
            sendMetricForTopic(invalidTradesTopic, 2);

            log.debug("Queue metrics sent to RTTM successfully");
        } catch (Exception ex) {
            log.warn("Failed to send queue metrics to RTTM: {}", ex.getMessage());
        }
    }

    /**
     * Send queue metric for a specific topic
     * In production, fetch real offset values from Kafka using KafkaConsumer
     */
    private void sendMetricForTopic(String topicName, int partitionId) {
        try {
            // These would normally be fetched from Kafka
            // For now, using placeholder values
            long producedOffset = System.currentTimeMillis() / 1000; // placeholder
            long consumedOffset = producedOffset - 10; // placeholder

            QueueMetricPayload metric = QueueMetricPayload.builder()
                    .serviceName("pms-validation")
                    .topicName(topicName)
                    .partitionId(partitionId)
                    .producedOffset(producedOffset)
                    .consumedOffset(consumedOffset)
                    .consumerGroup(consumerGroup)
                    .build();

            rttmClient.sendQueueMetric(metric);
            log.debug("Sent queue metric for topic {} partition {}", topicName, partitionId);
        } catch (Exception ex) {
            log.warn("Failed to send queue metric for {}: {}", topicName, ex.getMessage());
        }
    }
}
