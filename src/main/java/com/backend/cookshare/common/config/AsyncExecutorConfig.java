package com.backend.cookshare.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncExecutorConfig {

    /**
     * Executor cho recipe loading operations
     * Tối ưu cho 2 core CPU - Ưu tiên throughput
     */
    @Bean(name = "recipeLoaderExecutor")
    public Executor recipeLoaderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 2 threads cho I/O operations
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("recipe-loader-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // CallerRunsPolicy để tránh mất task khi queue đầy
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        log.info("Initialized recipeLoaderExecutor: core={}, max={}, queue={}",
                2, 4, 50);

        return executor;
    }

    /**
     * Executor cho report operations
     * Tối ưu cho 2 core CPU - Giảm thread để tránh context switching
     */
    @Bean(name = "reportAsyncExecutor")
    public Executor reportAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 3 threads cho report processing
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("report-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        log.info("Initialized reportAsyncExecutor: core={}, max={}, queue={}",
                3, 5, 100);

        return executor;
    }

    /**
     * Executor cho statistics operations
     * Tối ưu cho 2 core CPU - Giảm số thread
     */
    @Bean(name = "statisticsExecutor")
    public Executor statisticsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 2 threads cho statistics
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(75);
        executor.setThreadNamePrefix("statistics-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        log.info("Initialized statisticsExecutor: core={}, max={}, queue={}",
                2, 3, 75);

        return executor;
    }

    /**
     * Executor chung cho các tác vụ async khác
     * Default executor cho @Async annotation
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 2 threads core, 4 max cho general tasks
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        log.info("Initialized taskExecutor: core={}, max={}, queue={}",
                2, 4, 200);

        return executor;
    }
}