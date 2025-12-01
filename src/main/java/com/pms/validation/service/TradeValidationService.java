package com.pms.validation.service;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.dto.ValidationResult;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

@Service
public class TradeValidationService {

    private final KieContainer kieContainer;

    public TradeValidationService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    public ValidationResult validateTrade(TradeDto trade) {
        try {
            ValidationResult result = new ValidationResult();

            KieSession kieSession = kieContainer.newKieSession();

            try {
                kieSession.insert(trade);
                kieSession.insert(result);
                kieSession.fireAllRules();
            } finally {
                kieSession.dispose();
            }

            return result;
        } catch (Exception ex) {
            ValidationResult err = new ValidationResult();
            err.addError("Rule validation error: " + ex.getMessage());
            return err;
        }
    }
}
