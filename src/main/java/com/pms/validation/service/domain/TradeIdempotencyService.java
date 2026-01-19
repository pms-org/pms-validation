package com.pms.validation.service.domain;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class TradeIdempotencyService {

    private static final Duration PROCESSING_TTL = Duration.ofMinutes(5);
    private static final Duration DONE_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public TradeIdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(UUID tradeId) {
        return "trade:" + tradeId.toString();
    }

    public boolean tryStartProcessing(UUID tradeId) {
        String redisKey = key(tradeId);

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSING", PROCESSING_TTL);

        if (Boolean.TRUE.equals(acquired)) {
            log.debug("Redis lock ACQUIRED | key={} ttl={}min",
                    redisKey, PROCESSING_TTL.toMinutes());
            return true;
        }

        String existing = redisTemplate.opsForValue().get(redisKey);
        log.debug("Redis lock NOT acquired | key={} currentState={}",
                redisKey, existing);

        return false;
    }

    public void markDone(UUID tradeId) {
        String redisKey = key(tradeId);

        redisTemplate.opsForValue()
                .set(redisKey, "DONE", DONE_TTL);

        log.debug("Redis state set to DONE | key={} ttl={}days",
                redisKey, DONE_TTL.toDays());
    }

    public boolean isDone(UUID tradeId) {
        String redisKey = key(tradeId);
        String state = redisTemplate.opsForValue().get(redisKey);

        boolean done = "DONE".equals(state);

        if (done) {
            log.debug("Redis state DONE detected | key={}", redisKey);
        }

        return done;
    }
}
