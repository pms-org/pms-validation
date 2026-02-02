package com.pms.validation.service.outbox;

import org.springframework.stereotype.Component;

@Component
public class AdaptiveBatchSizer {

    private volatile int current = 10;

    public int getCurrentSize() {
        return current;
    }

    public void reset() {
        current = 3;
    }

    public void adjust(long durationMs, int batchSize) {
        // simple heuristic: if processing was fast, increase; if slow, decrease
        if (durationMs < 1000) {
            current = Math.min(100, current + 5);
        } else if (durationMs > 5000) {
            current = Math.max(1, current - 5);
        }
    }
}
