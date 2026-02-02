package com.pms.validation.service.outbox;

import java.util.List;

public record ProcessingResult<T>(List<Long> successfulIds, T poisonPill, boolean systemFailure) {

    public static <T> ProcessingResult<T> success(List<Long> ids) {
        return new ProcessingResult<>(ids, null, false);
    }

    public static <T> ProcessingResult<T> systemFailure(List<Long> successfulIds) {
        return new ProcessingResult<>(successfulIds, null, true);
    }

    public static <T> ProcessingResult<T> poisonPill(List<Long> successfulIds, T poison) {
        return new ProcessingResult<>(successfulIds, poison, false);
    }

}
