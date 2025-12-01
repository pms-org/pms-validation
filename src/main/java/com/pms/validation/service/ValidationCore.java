package com.pms.validation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.validation.dto.IngestionEventDto;
import com.pms.validation.dto.TradeDto;
import com.pms.validation.dto.ValidationResultDto;
import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.repository.ValidationOutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ValidationCore {

    private static final Logger logger = Logger.getLogger(KafkaConsumerService.class.getName());

    private ObjectMapper mapper;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private TradeValidationService tradeValidationService;

    @Autowired
    private ValidationOutboxRepository outboxRepo;

    // Atomic DB transaction: idempotency table insert + validate + outbox write.
    @Transactional
    public boolean handleTransaction(TradeDto trade) {

        boolean marked = idempotencyService.markAsProcessed(trade.getTradeId(), "ingestion-topic");
        if (!marked) {
            log.info("Trade already processed: {}", trade.getTradeId());
            return false;
        }

        ValidationResultDto result = tradeValidationService.validateTrade(trade);
        String status = result.isValid() ? "SUCCESS" : "FAILED";
        String errors = result.getErrors().isEmpty() ? null
                : result.getErrors().stream().collect(Collectors.joining("; "));

        ValidationOutboxEntity outbox = ValidationOutboxEntity.builder()
                .eventId(UUID.randomUUID())
                .tradeId(trade.getTradeId())
                .portfolioId(trade.getPortfolioId())
                .symbol(trade.getSymbol())
                .side(trade.getSide())
                .pricePerStock(trade.getPricePerStock())
                .quantity(trade.getQuantity())
                .tradeTimestamp(LocalDateTime.now())
                .sentStatus("PENDING") // Outbox message Status
                .validationStatus(status)
                .validationErrors(errors)
                .build();

        outboxRepo.save(outbox);

        log.info("Outbox entry inserted for trade {}", trade.getTradeId());
        return true;
    }

    public void processInfo(IngestionEventDto ingestionEvent) {
        try {
            TradeDto trade = mapper.readValue(ingestionEvent.getPayloadBytes(), TradeDto.class);

            if (idempotencyService.isAlreadyProcessed(trade.getTradeId())) {
                logger.info("Ignoring duplicate trade: " + trade.getTradeId());
                return;
            }

            logger.info("Delegating trade " + trade.getTradeId() + " to processor");
            boolean ok = handleTransaction(trade);
            if (!ok)
                return;

        } catch (Exception ex) {
            log.error("Error in IngestionProcessor.process: {}", ex.getMessage(), ex);
        }
    }
}
