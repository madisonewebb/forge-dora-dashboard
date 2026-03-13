package com.liatrio.dora.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    /**
     * Dedicated thread pool for blocking GitHub I/O operations.
     * Used by MetricsService to run parallel GitHub API fetches without
     * polluting ForkJoinPool.commonPool(), which is designed for CPU-bound work.
     */
    @Bean("ioExecutor")
    public Executor ioExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(10);
        exec.setMaxPoolSize(50);
        exec.setQueueCapacity(100);
        exec.setThreadNamePrefix("github-io-");
        exec.initialize();
        return exec;
    }
}
