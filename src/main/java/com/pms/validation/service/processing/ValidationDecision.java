package com.pms.validation.service.processing;

import com.pms.validation.entity.InvalidTradeEntity;
import com.pms.validation.entity.ValidationOutboxEntity;

public class ValidationDecision {
    private final boolean valid;
    private final ValidationOutboxEntity outboxEntity;
    private final InvalidTradeEntity invalidEntity;

    public ValidationDecision(ValidationOutboxEntity outboxEntity, InvalidTradeEntity invalidEntity) {
        this.outboxEntity = outboxEntity;
        this.invalidEntity = invalidEntity;
        this.valid = outboxEntity != null;
    }

    public boolean isValid() {
        return valid;
    }

    public ValidationOutboxEntity getOutboxEntity() {
        return outboxEntity;
    }

    public InvalidTradeEntity getInvalidEntity() {
        return invalidEntity;
    }
}
