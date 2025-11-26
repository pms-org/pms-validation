package com.pms.validation.entity;

import java.util.UUID;

import com.pms.validation.enums.TradeSide;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data

@Entity
@Table(name = "validated_outbox")
public class ValidationOutbox {
    @Id
    @Column(name = "trade_id")
    private UUID tradeId;

    @Column(name = "cusip_id")
    private String cusipId;

    @Column(name = "cusip_name")
    private String cusipName;

    @Enumerated(EnumType.STRING)
    @Column(name = "side")
    private TradeSide side;

    @Column(name = "price_per_stock")
    private double pricePerStock;

    @Column(name = "quantity")
    private long quantity;

    @Column(name = "timestamp")
    private String timestamp;

    @Column(name = "status")
    private String status;
}
