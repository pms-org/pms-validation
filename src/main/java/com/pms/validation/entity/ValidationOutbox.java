package com.pms.validation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.pms.validation.enums.Sector;
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
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data

@Entity
@Table(name = "validation_outbox")
public class ValidationOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "validation_outbox_id")
    private Long validationOutboxId;

    @Column(name = "trade_id")
    private UUID tradeId;

    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "sector_name")
    private Sector sectorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "side")
    private TradeSide side;

    @Column(name = "price_per_stock")
    private BigDecimal pricePerStock;

    @Column(name = "quantity")
    private long quantity;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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