package com.pms.validation.service;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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

    // Atomic DB transaction: idempotency table insert + validate + outbox write.
    @Transactional
    public boolean handleTransaction(TradeDto trade) {

        boolean marked = idempotencyService.markAsProcessed(trade.getTradeId(), "ingestion-topic");
        if (!marked) {
            log.info("Trade already processed: {}", trade.getTradeId());
            return false;
        }

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
        return true;
    }

    public void processTrade(TradeDto trade) {
        try {

            if (idempotencyService.isAlreadyProcessed(trade.getTradeId())) {
                log.info("Ignoring duplicate trade: " + trade.getTradeId());
                return;
            }

            log.info("Delegating trade " + trade.getTradeId() + " to processor");
            boolean ok = handleTransaction(trade);
            if (!ok) {
                return;
            }

        } catch (Exception ex) {
            log.error("Error in IngestionProcessor.process: {}", ex.getMessage(), ex);
        }
    }
}
