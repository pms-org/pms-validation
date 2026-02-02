package com.pms.validation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadExecutorConfig {

    private static final Logger log = LoggerFactory.getLogger(ThreadExecutorConfig.class);

    @Bean(name = "batchExecutor")
    public ThreadPoolTaskExecutor batchProcessorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("batch-processing-exec-");
        executor.initialize();
        log.info("Initialized batchProcessorExecutor with corePoolSize={} maxPoolSize={} queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getThreadPoolExecutor().getQueue().remainingCapacity());
        return executor;
    }

    @Bean(name = "outboxExecutor")
    public ThreadPoolTaskExecutor outboxExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("outbox-executor-");
        executor.initialize();
        return executor;
    }
}
