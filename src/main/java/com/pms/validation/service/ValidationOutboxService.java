package com.pms.validation.service;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.dto.ValidationOutputDto;
import com.pms.validation.dto.ValidationResultDto;
import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.repository.ValidationOutboxRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ValidationOutboxService {

    @Autowired
    private ValidationOutboxRepository repository;

    @Transactional
    public ValidationOutboxEntity saveValidationEvent(TradeDto trade, ValidationResultDto result, String status) {
        ValidationOutboxEntity outbox = new ValidationOutboxEntity();
        outbox.setTradeId(trade.getTradeId());
        outbox.setPortfolioId(trade.getPortfolioId());
        outbox.setSymbol(trade.getSymbol());
        outbox.setSide(trade.getSide());
        outbox.setPricePerStock(trade.getPricePerStock());
        outbox.setQuantity(trade.getQuantity());
        outbox.setTimestamp(trade.getTimestamp());
        outbox.setStatus(status);
        outbox.setCreatedAt(LocalDateTime.now());
        outbox.setUpdatedAt(LocalDateTime.now());

        System.out.println("Saving ValidationOutbox: " + outbox);
        return repository.save(outbox);
    }

    public ValidationOutputDto buildValidationEvent(TradeDto trade, ValidationResultDto result) {
        String status = result.isValid() ? "SUCCESS" : "FAILED";
        String errors = result.getErrors().isEmpty() ? null
                : result.getErrors().stream().collect(Collectors.joining("; "));

        return ValidationOutputDto.builder()
                .eventId(UUID.randomUUID())
                .tradeId(trade.getTradeId())
                .portfolioId(trade.getPortfolioId())
                .symbol(trade.getSymbol())
                .side(trade.getSide())
                .pricePerStock(trade.getPricePerStock())
                .quantity(trade.getQuantity())
                .tradeTimestamp(trade.getTimestamp())
                .validationStatus(status)
                .validationErrors(errors)
                .processedAt(LocalDateTime.now())
                .build();
    }
}
