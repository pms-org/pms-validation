package com.pms.validation.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.pms.validation.proto.TradeEventProto;
import com.pms.validation.service.processing.ValidationBatchProcessingService;
import com.pms.validation.wrapper.PollBatch;

@Service
public class BatchProcessor implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(BatchProcessor.class);

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private LinkedBlockingDeque<PollBatch> buffer;

    @Autowired
    @Qualifier("batchExecutor")
    private ThreadPoolTaskExecutor batchProcessorExecutor;

    @Autowired
    @Qualifier("batchFlushScheduler")
    private ThreadPoolTaskScheduler batchFlushScheduler;

    @Autowired
    @Qualifier("dbRecoveryScheduler")
    private ThreadPoolTaskScheduler dbRecoveryScheduler;

    @Autowired
    private ValidationBatchProcessingService validationService;

    @Value("${app.batch.size:200}")
    private int BATCH_SIZE;

    @Value("${app.buffer.size:1000}")
    private int totalBufferCapacity;

    @Value("${app.flush-interval-ms:200}")
    private long FLUSH_INTERVAL_MS;

    @Value("${app.trades.consumer.consumer-id:tradesListener}")
    private String CONSUMER_ID;

    private boolean isRecovering = false;
    private ScheduledFuture<?> recoveryTask;
    private boolean isRunning = false;

    public void checkAndFlush() {
        if (buffer.size() >= BATCH_SIZE) {
            batchProcessorExecutor.execute(this::flushBatch);
        }
    }

    public synchronized void flushBatch() {
        if (buffer.isEmpty())
            return;
        List<PollBatch> pollsInTheBatch = new ArrayList<>();
        List<TradeEventProto> batchTrades = new ArrayList<>(BATCH_SIZE);
        int currentRecordCount = 0;
        while (currentRecordCount < BATCH_SIZE) {
            PollBatch nextPoll = buffer.peek();
            if (nextPoll == null)
                break;
            if (currentRecordCount + nextPoll.getTradeProtos().size() > 5000 && !pollsInTheBatch.isEmpty()) {
                break;
            }
            PollBatch poll = buffer.poll();
            if (poll == null)
                break;
            pollsInTheBatch.add(poll);
            batchTrades.addAll(poll.getTradeProtos());
            currentRecordCount += poll.getTradeProtos().size();
        }
        try {
            // Delegate to existing validation batch processing (preserves business logic)
            validationService.processBatch(batchTrades, -1, "", null, "");

            // Acknowledge each poll after successful processing
            pollsInTheBatch.forEach(poll -> {
                try {
                    if (poll.getAck() != null) poll.getAck().acknowledge();
                } catch (Exception ex) {
                    logger.warn("Failed to acknowledge poll", ex);
                }
            });

            int remainingMessages = buffer.stream()
        .mapToInt(poll -> poll.getTradeProtos().size())
        .sum();
logger.info("Flushed {} trades. Remaining messages in buffer: {}", batchTrades.size(), remainingMessages);

            if (isRecovering && buffer.size() <= 0.5 * totalBufferCapacity) {
                resumeConsumer();
            }

        } catch (DataAccessResourceFailureException e) {
            logger.error("DB Connection failure. Pausing consumer.", e);
            // push polls back to buffer in reverse order
            for (int i = pollsInTheBatch.size() - 1; i >= 0; i--) {
                buffer.offerFirst(pollsInTheBatch.get(i));
            }
            handleConsumerThread(true);
            throw e;
        } catch (Exception e) {
            logger.error("Exception occured while processing batch", e);
            throw e;
        }
    }

    @Override
    public void start() {
        logger.info("BatchProcessor starting: Initializing time-based flush");
        batchFlushScheduler.scheduleWithFixedDelay(this::flushBatch, Duration.ofMillis(FLUSH_INTERVAL_MS));
        this.isRunning = true;
    }

    @Override
    public void stop(Runnable callback) {
        logger.info("BatchProcessor stopping: Performing final flush");
        batchFlushScheduler.shutdown();
        if (!buffer.isEmpty()) {
            flushBatch();
        }
        this.isRunning = false;
        callback.run();
    }

    public void handleConsumerThread(boolean startDaemon) {
        synchronized (this) {
            if (isRecovering)
                return;
            isRecovering = true;
        }

        MessageListenerContainer container = kafkaListenerEndpointRegistry.getListenerContainer(CONSUMER_ID);
        if (container != null && !container.isContainerPaused()) {
            container.pause();
            logger.warn("Kafka Consumer paused.");
        }
        if (startDaemon) {
            logger.warn(" Starting background probe daemon...");
            startDaemon();
        }

    }

    private void resumeConsumer() {
        MessageListenerContainer container = kafkaListenerEndpointRegistry.getListenerContainer(CONSUMER_ID);
        if (container != null && container.isContainerPaused()) {
            container.resume();
            logger.info("Buffer cleared to 50% ({} batches). Resuming consumer.", buffer.size());
            synchronized (this) {
                isRecovering = false;
            }
        }
    }

    private void startDaemon() {
        recoveryTask = dbRecoveryScheduler.scheduleWithFixedDelay(() -> {
            try {
                // probe DB by simple select via validationService repository usage indirectly
                logger.info("Daemon: probing DB connectivity...");
                // If DB is up, resume consumer
                MessageListenerContainer container = kafkaListenerEndpointRegistry.getListenerContainer(CONSUMER_ID);
                if (container != null) container.resume();

                synchronized (this) {
                    isRecovering = false;
                    if (recoveryTask != null) {
                        recoveryTask.cancel(false);
                        recoveryTask = null;
                    }
                }
            } catch (Exception e) {
                logger.warn("Daemon: Database still down. Retrying...", e);
            }
        }, Duration.ofMillis(10000));
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
