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
    private ValidationCore validationCore;

    public void processTrade(TradeDto trade) {

        log.info("Processing trade | tradeId={}", trade.getTradeId());
        try {
            
            validationCore.handleTransaction(trade);
            log.info("Trade processed successfully | tradeId={}",
                    trade.getTradeId());

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
