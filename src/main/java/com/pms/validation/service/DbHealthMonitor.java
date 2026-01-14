package com.pms.validation.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DbHealthMonitor {

    private final JdbcTemplate jdbcTemplate;
    private final KafkaListenerEndpointRegistry registry;

    private volatile boolean paused = false;
    private volatile boolean monitoring = false;

    public synchronized void pause() {
        if (!paused) {
            log.info("Pausing kafka consumer (DB down)");
            paused = true;
        }

        if (!monitoring) {
            log.info("Starting DB health monitor thread");
            monitoring = true;
            monitorAndResume();
        }
    }

    @Async
    public void monitorAndResume() {
        try {
            while (paused) {
                if (databaseIsUp()) {
                    log.info("DB is back — Resuming Kafka consumption");
                    // listener id used in KafkaConsumerService
                    var container = registry.getListenerContainer("tradesListener");
                    if (container != null) {
                        container.start();
                    } else {
                        log.warn("Listener container 'tradesListener' not found");
                    }
                    paused = false;
                    monitoring = false;
                    return;
                }

                log.info("DB still down, retrying in 5s");
                Thread.sleep(5000);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            monitoring = false;
        }
    }

    private boolean databaseIsUp() {
        try {
            // lightweight check
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
