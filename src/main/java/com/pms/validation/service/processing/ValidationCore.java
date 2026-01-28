package com.pms.validation.service.processing;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
// transactional behavior is handled by the caller when batching

import com.pms.validation.dto.TradeDto;
import com.pms.validation.dto.ValidationResultDto;
import com.pms.validation.entity.InvalidTradeEntity;
import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.repository.InvalidTradeRepository;
import com.pms.validation.repository.ValidationOutboxRepository;
import com.pms.validation.service.domain.TradeValidationService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ValidationCore {

    @Autowired
    private TradeValidationService tradeValidationService;

    @Autowired
    private ValidationOutboxRepository outboxRepo;

    @Autowired
    private InvalidTradeRepository invalidTradeRepo;

    @Value("${app.incoming-trades-topic}")
    private String incomingTradesTopic;

    /**
     * Evaluate a trade against validation rules and build entity objects without
     * persisting.
     * The caller is responsible for persisting the returned entities (batch saveAll
     * recommended).
     */
    public ValidationDecision evaluate(TradeDto trade) {
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

            log.debug("Validation decision: VALID for trade {}", trade.getTradeId());
            return new ValidationDecision(outbox, null);

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

            log.debug("Validation decision: INVALID for trade {}", trade.getTradeId());
            return new ValidationDecision(null, invalidTrade);
        }
    }
}
