package com.pms.validation.service;

import com.pms.validation.dto.IngestionEventDto;
import com.pms.validation.dto.TradeDto;
import com.pms.validation.dto.ValidationResultDto;
import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.repository.ValidationOutboxRepository;
import com.pms.validation.enums.TradeSide;
import com.pms.validation.enums.Sector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class IngestionProcessor {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private TradeValidationService tradeValidationService;

    @Autowired
    private ValidationOutboxRepository outboxRepo;

    @Transactional
    public boolean handleTransaction(TradeDto trade) {

        boolean marked = idempotencyService.markAsProcessed(trade.getTradeId(), "ingestion-topic");
        if (!marked) {
            log.info("Trade already processed: {}", trade.getTradeId());
            return false;
        }

        ValidationResultDto result = tradeValidationService.validateTrade(trade);
        String status = result.isValid() ? "SUCCESS" : "FAILED";

        ValidationOutboxEntity outbox = ValidationOutboxEntity.builder()
                .tradeId(trade.getTradeId())
                .portfolioId(trade.getPortfolioId())
                .symbol(trade.getSymbol())
                .side(trade.getSide())
                .pricePerStock(trade.getPricePerStock())
                .quantity(trade.getQuantity())
                .timestamp(LocalDateTime.now())
                .status("PENDING") // Outbox Pattern
                .build();

        outboxRepo.save(outbox);

        log.info("Outbox entry inserted for trade {}", trade.getTradeId());
        return true;
    }

    public void processInfo(IngestionEventDto ingestionEvent, TradeDto trade) {
        try {
            boolean ok = handleTransaction(trade);
            if (!ok)
                return;

        } catch (Exception ex) {
            log.error("Error in IngestionProcessor.process: {}", ex.getMessage(), ex);
        }
    }
}
