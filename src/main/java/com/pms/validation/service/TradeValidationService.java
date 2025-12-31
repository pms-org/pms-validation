package com.pms.validation.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.pool2.ObjectPool;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.dto.ValidationResultDto;
import com.pms.validation.repository.InvestorDetailsRepository;
import com.pms.validation.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TradeValidationService {

    private final ObjectPool<KieSession> kieSessionPool;

    private final InvestorDetailsRepository investorDetailsRepository;

    private final StockRepository stockRepository;

    public TradeValidationService(ObjectPool<KieSession> kieSessionPool,
            InvestorDetailsRepository investorDetailsRepository, StockRepository stockRepository) {
        this.kieSessionPool = kieSessionPool;
        this.investorDetailsRepository = investorDetailsRepository;
        this.stockRepository = stockRepository;
    }

    public ValidationResultDto validateTrade(TradeDto trade) {
        KieSession kieSession = null;
        try {
            kieSession = kieSessionPool.borrowObject();

            List<UUID> validPortfolios = investorDetailsRepository.findAll()
                    .stream()
                    .map(investor -> investor.getPortfolioId())
                    .collect(Collectors.toList());

            List<String> validSymbols = stockRepository.findAll()
                    .stream()
                    .map(stock -> stock.getSymbol())
                    .collect(Collectors.toList());

            ValidationResultDto result = new ValidationResultDto();

            kieSession.insert(trade);
            kieSession.insert(result);
            kieSession.setGlobal("validPortfolios", validPortfolios);
            kieSession.setGlobal("validSymbols", validSymbols);

            kieSession.fireAllRules();

            kieSession.getFactHandles().forEach(kieSession::delete);

            return result;
        } catch (RuntimeException ex) {
            log.error("Runtime error during trade validation: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error during trade validation: {}", ex.getMessage());
            throw new RuntimeException("Validation error", ex);
        } finally {
            if (kieSession != null) {
                try {
                    kieSessionPool.returnObject(kieSession);
                } catch (Exception e) {
                    log.warn("Failed returning KieSession to pool: {}", e.getMessage());
                }
            }
        }
    }
}
