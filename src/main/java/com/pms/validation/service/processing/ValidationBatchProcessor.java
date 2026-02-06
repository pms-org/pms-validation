package com.pms.validation.service.processing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.pms.validation.proto.TradeEventProto;
import com.pms.validation.wrapper.PollBatch;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ValidationBatchProcessor implements SmartLifecycle {

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LinkedBlockingDeque<PollBatch> validationBuffer;

    @Autowired
    @Qualifier("validationBatchExecutor")
    private ThreadPoolTaskExecutor batchExecutor;

    @Autowired
    @Qualifier("validationBatchFlushScheduler")
    private ThreadPoolTaskScheduler batchFlushScheduler;

    @Autowired
    @Qualifier("validationDbRecoveryScheduler")
    private ThreadPoolTaskScheduler dbRecoveryScheduler;

    @Autowired
    private ValidationBatchProcessingService validationBatchProcessingService;

    @Value("${app.validation.batch.size:1000}")
    private int BATCH_SIZE;

    @Value("${app.buffer.size:50}")
    private int totalBufferCapacity;

    @Value("${app.validation.flush-interval-ms:5000}")
    private long FLUSH_INTERVAL_MS;

    private static final String CONSUMER_ID = "tradesListener";

    private boolean isRecovering = false;
    private ScheduledFuture<?> recoveryTask;
    private boolean isRunning = false;

    public void checkAndFlush() {
        if (validationBuffer.size() >= BATCH_SIZE) {
            batchExecutor.execute(this::flushBatch);
        }
    }

    public synchronized void flushBatch() {
        if (validationBuffer.isEmpty()) {
            return;
        }

        log.info("Flushing validation batch. Buffer size: {}", validationBuffer.size());

        List<PollBatch> pollsInTheBatch = new ArrayList<>();
        List<TradeEventProto> batchTrades = new ArrayList<>(BATCH_SIZE);
        int currentRecordCount = 0;

        // Collect trades from buffer until batch size is reached
        while (currentRecordCount < BATCH_SIZE) {
            PollBatch nextPoll = validationBuffer.peek();
            if (nextPoll == null) {
                break;
            }

            // Prevent exceeding max batch size
            if (currentRecordCount + nextPoll.getTradeProtos().size() > BATCH_SIZE && !pollsInTheBatch.isEmpty()) {
                break;
            }

            PollBatch poll = validationBuffer.poll();
            pollsInTheBatch.add(poll);
            batchTrades.addAll(poll.getTradeProtos());
            currentRecordCount += poll.getTradeProtos().size();
        }

        if (pollsInTheBatch.isEmpty()) {
            return;
        }

        try {
            // Use metadata from the first poll batch for topic and consumer group
            PollBatch firstPoll = pollsInTheBatch.get(0);

            // Collect all partitions and offsets from all polls
            List<Integer> allPartitions = new ArrayList<>();
            List<Long> allOffsets = new ArrayList<>();
            for (PollBatch poll : pollsInTheBatch) {
                if (poll.getPartitions() != null) {
                    allPartitions.addAll(poll.getPartitions());
                }
                if (poll.getOffsets() != null) {
                    allOffsets.addAll(poll.getOffsets());
                }
            }

            // Process the batch
            validationBatchProcessingService.processBatch(
                    batchTrades,
                    allPartitions,
                    firstPoll.getTopic(),
                    allOffsets,
                    firstPoll.getConsumerGroup());

            // Acknowledge all polls in the batch
            pollsInTheBatch.forEach(poll -> poll.getAck().acknowledge());

            log.info("Successfully processed and acknowledged {} trade events", batchTrades.size());

            // If recovering and buffer is below 50%, resume consumer
            if (isRecovering && validationBuffer.size() <= 0.5 * totalBufferCapacity) {
                resumeConsumer();
            }

        } catch (DataAccessResourceFailureException e) {
            log.error("DB Connection failure. Pausing consumer and returning batches to buffer.");

            // Return batches to front of buffer in reverse order
            for (int i = pollsInTheBatch.size() - 1; i >= 0; i--) {
                validationBuffer.offerFirst(pollsInTheBatch.get(i));
            }

            handleConsumerThread(true);
            throw e;

        } catch (Exception e) {
            log.error("Exception occurred during batch processing: {}", e.getMessage(), e);

            // Return batches to front of buffer in reverse order
            for (int i = pollsInTheBatch.size() - 1; i >= 0; i--) {
                validationBuffer.offerFirst(pollsInTheBatch.get(i));
            }

            throw e;
        }
    }

    @Override
    public void start() {
        log.info("ValidationBatchProcessor starting: Initializing time-based flush");
        batchFlushScheduler.scheduleWithFixedDelay(this::flushBatch, Duration.ofMillis(FLUSH_INTERVAL_MS));
        this.isRunning = true;
    }

    @Override
    public void stop(Runnable callback) {
        log.info("ValidationBatchProcessor stopping: Performing final flush");
        batchFlushScheduler.shutdown();

        if (!validationBuffer.isEmpty()) {
            flushBatch();
        }

        this.isRunning = false;
        callback.run();
    }

    public void handleConsumerThread(boolean startDaemon) {
        synchronized (this) {
            if (isRecovering) {
                return;
            }
            isRecovering = true;
        }

        MessageListenerContainer container = kafkaListenerEndpointRegistry.getListenerContainer(CONSUMER_ID);
        if (container != null && !container.isContainerPaused()) {
            container.pause();
            log.warn("Kafka Consumer paused.");
        }

        if (startDaemon) {
            log.warn("Starting background probe daemon...");
            startDaemon();
        }
    }

    private void resumeConsumer() {
        MessageListenerContainer container = kafkaListenerEndpointRegistry.getListenerContainer(CONSUMER_ID);
        if (container != null && container.isContainerPaused()) {
            container.resume();
            log.info("Buffer cleared to 50% ({} batches). Resuming consumer.", validationBuffer.size());

            synchronized (this) {
                isRecovering = false;
            }
        }
    }

    private void startDaemon() {
        recoveryTask = dbRecoveryScheduler.scheduleWithFixedDelay(() -> {
            try {
                jdbcTemplate.execute("SELECT 1");
                log.info("Database is up! Resuming consumer and stopping daemon.");

                MessageListenerContainer container = kafkaListenerEndpointRegistry.getListenerContainer(CONSUMER_ID);
                if (container != null) {
                    container.resume();
                }

                synchronized (this) {
                    isRecovering = false;
                    if (recoveryTask != null) {
                        recoveryTask.cancel(false);
                        recoveryTask = null;
                    }
                }
            } catch (Exception e) {
                log.warn("Daemon: Database still down. Retrying in 10s...");
            }
        }, Duration.ofMillis(10000));
    }

    @Override
    public void stop() {
        // No-op, required by SmartLifecycle
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
