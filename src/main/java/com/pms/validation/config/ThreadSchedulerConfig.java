package com.pms.validation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class ThreadSchedulerConfig {
    
    @Bean(name = "validationBatchFlushScheduler")
    public ThreadPoolTaskScheduler validationBatchFlushScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("validation-time-flush-");
        scheduler.setPoolSize(1);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }

    @Bean(name = "validationDbRecoveryScheduler")
    public ThreadPoolTaskScheduler validationDbRecoveryScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("validation-db-recovery-");
        scheduler.setPoolSize(1);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }
}
