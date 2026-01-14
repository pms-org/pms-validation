package com.pms.validation.service;

import java.util.List;

import com.pms.validation.entity.ValidationOutboxEntity;

public record ProcessingResult(List<Long> successfulIds, ValidationOutboxEntity poisonPill, boolean systemFailure) {

    public static ProcessingResult success(List<Long> ids) {
        return new ProcessingResult(ids, null, false);
    }

    public static ProcessingResult systemFailure(List<Long> successfulIds) {
        return new ProcessingResult(successfulIds, null, true);
    }

    public static ProcessingResult poisonPill(List<Long> successfulIds, ValidationOutboxEntity poison) {
        return new ProcessingResult(successfulIds, poison, false);
    }

}
