package com.pms.validation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.pms.validation.enums.TradeSide;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "validation_validation_outbox")
public class ValidationOutboxEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "validation_outbox_id")
    private Long validationOutboxId;

    // @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "trade_id", nullable = false)
    private UUID tradeId;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false)
    private TradeSide side;

    @Column(name = "price_per_stock", nullable = false)
    private BigDecimal pricePerStock;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "trade_timestamp", nullable = false)
    private LocalDateTime tradeTimestamp;

    @Column(name = "sent_status", nullable = false)
    private String sentStatus;

    @Column(name = "validation_status", nullable = false)
    private String validationStatus;

    @Column(name = "validation_errors", nullable = true)
    private String validationErrors; // Semi colon separated errors

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
