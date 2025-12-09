package com.pms.validation.mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.google.protobuf.Timestamp;
import com.pms.validation.entity.InvalidTradeEntity;
import com.pms.validation.proto.TradeEventProto;

@Component
public class ProtoInvalidTradeEntityMapper {

    // --------------------- ENTITY → PROTO ---------------------
    public static TradeEventProto toProto(InvalidTradeEntity entity) {

        TradeEventProto.Builder builder = TradeEventProto.newBuilder();

        if (entity.getPortfolioId() != null) {
            builder.setPortfolioId(entity.getPortfolioId().toString());
        }

        if (entity.getTradeId() != null) {
            builder.setTradeId(entity.getTradeId().toString());
        }

        if (entity.getSymbol() != null) {
            builder.setSymbol(entity.getSymbol());
        }

        if (entity.getSide() != null) {
            builder.setSide(entity.getSide().name());
        }

        if (entity.getPricePerStock() != null) {
            builder.setPricePerStock(entity.getPricePerStock().doubleValue());
        }

        if (entity.getQuantity() != null) {
            builder.setQuantity(entity.getQuantity());
        }

        if (entity.getTradeTimestamp() != null) {
            builder.setTimestamp(convertLocalDateTime(entity.getTradeTimestamp()));
        }

        return builder.build();
    }

    // --------------------- PROTO → ENTITY ---------------------
    public static InvalidTradeEntity toEntity(TradeEventProto proto) {

        return InvalidTradeEntity.builder()
                .portfolioId(proto.getPortfolioId().isEmpty() ? null : UUID.fromString(proto.getPortfolioId()))
                .tradeId(proto.getTradeId().isEmpty() ? null : UUID.fromString(proto.getTradeId()))
                .symbol(proto.getSymbol().isEmpty() ? null : proto.getSymbol())
                .side(proto.getSide().isEmpty() ? null
                        : Enum.valueOf(com.pms.validation.enums.TradeSide.class, proto.getSide()))
                .pricePerStock(
                        proto.getPricePerStock() == 0 ? null : java.math.BigDecimal.valueOf(proto.getPricePerStock()))
                .quantity(proto.getQuantity() == 0 ? null : proto.getQuantity())
                .tradeTimestamp(proto.hasTimestamp() ? convertTimestamp(proto.getTimestamp()) : null)
                .build();
    }

    // --------------------- Timestamp Converters ---------------------
    private static LocalDateTime convertTimestamp(Timestamp ts) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()),
                ZoneId.systemDefault());
    }

    private static Timestamp convertLocalDateTime(LocalDateTime ldt) {
        Instant instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
