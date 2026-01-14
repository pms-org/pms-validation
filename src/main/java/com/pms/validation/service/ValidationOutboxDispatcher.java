package com.pms.validation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ValidationOutboxDispatcher implements SmartLifecycle {

    @Autowired
    private ValidationOutboxEventProcessor processor;

    @Autowired
    private AdaptiveBatchSizer batchSizer;

    private volatile boolean running = false;

    @Override
    public void start() {
        running = true;
        Thread t = new Thread(this::loop, "validation-outbox-dispatcher");
        t.setDaemon(true);
        t.start();
    }

    private void loop() {
        while (running) {
            try {
                log.info("Validation Outbox Poller is running....");
                ProcessingResult result = processor.dispatchOnce();

                if (result.systemFailure()) {
                    batchSizer.reset();
                    Thread.sleep(2000);
                } else if (result.successfulIds().isEmpty() && result.poisonPill() == null) {
                    Thread.sleep(5000);
                }
                log.info("Validation Poller completed polling...");
            } catch (Exception e) {
                log.error("Validation Outbox dispatcher failed", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    log.error("InterruptedException occured ", ignored);
                }
            }
        }
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
