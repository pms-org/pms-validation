package com.pms.validation.config;

import java.util.concurrent.LinkedBlockingDeque;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pms.validation.wrapper.PollBatch;

@Configuration
public class BufferConfig {
    
    @Value("${app.buffer.size:50}")
    private int bufferSize;
    
    @Bean
    public LinkedBlockingDeque<PollBatch> validationBuffer() {
        return new LinkedBlockingDeque<>(bufferSize);
    }
}
