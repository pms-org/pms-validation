package com.pms.validation.service;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.dto.ValidationResultDto;
import com.pms.validation.entity.InvalidTradeEntity;
import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.repository.InvalidTradeRepository;
import com.pms.validation.repository.ValidationOutboxRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ValidationCore {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private TradeValidationService tradeValidationService;

    @Autowired
    private ValidationOutboxRepository outboxRepo;

    @Autowired
    private InvalidTradeRepository invalidTradeRepo;

    @Value("${app.incoming-topic}")
    private String incomingTopic;

    // Atomic DB transaction: idempotency table insert + validate + outbox write.
    @Transactional
    public void handleTransaction(TradeDto trade) {

        idempotencyService.markAsProcessed(trade.getTradeId(), incomingTopic);

        ValidationResultDto result = tradeValidationService.validateTrade(trade);

        String status = result.isValid() ? "VALID" : "INVALID";
        String errors = result.getErrors().isEmpty() ? null
                : result.getErrors().stream().collect(Collectors.joining("; "));

        if (status.equals("VALID")) {
            log.info("Trade {} is valid.", trade.getTradeId());

            ValidationOutboxEntity outbox = ValidationOutboxEntity.builder()
                    .eventId(UUID.randomUUID())
                    .tradeId(trade.getTradeId())
                    .portfolioId(trade.getPortfolioId())
                    .symbol(trade.getSymbol())
                    .side(trade.getSide())
                    .pricePerStock(trade.getPricePerStock())
                    .quantity(trade.getQuantity())
                    .tradeTimestamp(trade.getTimestamp())
                    .sentStatus("PENDING") // Outbox message Status
                    .validationStatus(status)
                    .validationErrors(null)
                    .build();

            outboxRepo.save(outbox);

        } else {
            log.info("Trade {} is invalid: {}", trade.getTradeId(), errors);

            InvalidTradeEntity invalidTrade = InvalidTradeEntity.builder()
                    .eventId(UUID.randomUUID())
                    .tradeId(trade.getTradeId())
                    .portfolioId(trade.getPortfolioId())
                    .symbol(trade.getSymbol())
                    .side(trade.getSide())
                    .pricePerStock(trade.getPricePerStock())
                    .quantity(trade.getQuantity())
                    .tradeTimestamp(trade.getTimestamp())
                    .sentStatus("PENDING")
                    .validationStatus(status)
                    .validationErrors(errors)
                    .build();

            invalidTradeRepo.save(invalidTrade);

        }

        log.info("Outbox entry inserted for trade {}", trade.getTradeId());

    }
}
