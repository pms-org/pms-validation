package com.pms.validation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "processed_messages", uniqueConstraints = {
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
