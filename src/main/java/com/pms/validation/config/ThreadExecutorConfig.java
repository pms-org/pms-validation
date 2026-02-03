package com.pms.validation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadExecutorConfig {
    
    @Bean(name = "validationBatchExecutor")
    public ThreadPoolTaskExecutor validationBatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);   
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1); 
        executor.setThreadNamePrefix("validation-batch-exec-");
        executor.initialize();
        return executor;
    }
}
