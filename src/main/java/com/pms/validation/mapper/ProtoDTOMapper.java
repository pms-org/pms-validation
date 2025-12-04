package com.pms.validation.mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.google.protobuf.Timestamp;
import com.pms.validation.dto.TradeDto;
import com.pms.validation.enums.TradeSide;
import com.pms.validation.proto.TradeEventProto;

@Component
public class ProtoDTOMapper {

    // ---------- PROTO -> DTO ----------
    public static TradeDto toDto(TradeEventProto proto) {

        return TradeDto.builder()
                .tradeId(proto.getTradeId().isEmpty() ? null : UUID.fromString(proto.getTradeId()))
                .portfolioId(proto.getPortfolioId().isEmpty() ? null : UUID.fromString(proto.getPortfolioId()))
                .symbol(proto.getSymbol().isEmpty() ? null : proto.getSymbol())
                .side(proto.getSide().isEmpty() ? null : TradeSide.valueOf(proto.getSide()))
                .pricePerStock(proto.getPricePerStock() == 0 ? null :
                        java.math.BigDecimal.valueOf(proto.getPricePerStock()))
                .quantity(proto.getQuantity() == 0 ? null : proto.getQuantity())
                .timestamp(proto.hasTimestamp() ? convertTimestamp(proto.getTimestamp()) : null)
                .build();
    }

    
    // ---------- DTO -> PROTO ----------
    public static TradeEventProto toProto(TradeDto dto) {

        TradeEventProto.Builder builder = TradeEventProto.newBuilder();

        if (dto.getTradeId() != null) {
            builder.setTradeId(dto.getTradeId().toString());
        }

        if (dto.getPortfolioId() != null) {
            builder.setPortfolioId(dto.getPortfolioId().toString());
        }

        if (dto.getSymbol() != null) {
            builder.setSymbol(dto.getSymbol());
        }

        if (dto.getSide() != null) {
            builder.setSide(dto.getSide().name());
        }

        if (dto.getPricePerStock() != null) {
            builder.setPricePerStock(dto.getPricePerStock().doubleValue());
        }

        if (dto.getQuantity() != null) {
            builder.setQuantity(dto.getQuantity());
        }

        if (dto.getTimestamp() != null) {
            builder.setTimestamp(convertLocalDateTime(dto.getTimestamp()));
        }

        return builder.build();
    }

    // ---------- Converters ----------
    private static LocalDateTime convertTimestamp(Timestamp ts) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()),
                ZoneId.systemDefault()
        );
    }

    private static Timestamp convertLocalDateTime(LocalDateTime ldt) {
        Instant instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
