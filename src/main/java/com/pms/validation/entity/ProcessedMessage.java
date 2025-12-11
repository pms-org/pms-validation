package com.pms.validation.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "validation_processed_messages", uniqueConstraints = {
        @UniqueConstraint(name = "uk_trade_id_consumer_group", columnNames = { "trade_id", "consumer_group" })
})
public class ProcessedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_id", nullable = false)
    private UUID tradeId;

    @Column(name = "consumer_group", nullable = false)
    private String consumerGroup;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public ProcessedMessage(UUID tradeId, String consumerGroup, String topic) {
        this.tradeId = tradeId;
        this.consumerGroup = consumerGroup;
        this.topic = topic;
        this.processedAt = LocalDateTime.now();
    }
}
