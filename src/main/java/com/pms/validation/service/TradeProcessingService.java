package com.pms.validation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.exception.NonRetryableException;
import com.pms.validation.exception.RetryableException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TradeProcessingService {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private ValidationCore validationCore;

    public void processTrade(TradeDto trade) {
        try {

            if (idempotencyService.isAlreadyProcessed(trade.getTradeId())) {
                log.info("Ignoring duplicate trade: " + trade.getTradeId());
                return;
            }

            log.info("Delegating trade " + trade.getTradeId() + " to processor");

            validationCore.handleTransaction(trade);

        } catch (NonRetryableException ex) {
            log.error("Non-retryable error in processTrade for {}: {}", trade.getTradeId(), ex.getMessage(), ex);
            throw ex; // goes straight to DLT, no retry
        } catch (RetryableException ex) {
            log.error("Retryable error in processTrade for {}: {}", trade.getTradeId(), ex.getMessage(), ex);
            throw ex; // triggers @RetryableTopic retries
        } catch (RuntimeException ex) {
            log.error("Unexpected runtime error in processTrade for {}: {}", trade.getTradeId(), ex.getMessage(), ex);
            throw new RetryableException("Unexpected runtime error", ex);
        } catch (Exception ex) {
            log.error("Unexpected checked error in processTrade for {}: {}", trade.getTradeId(), ex.getMessage(), ex);
            throw new RetryableException("Unexpected checked error", ex);
        }
    }
}
