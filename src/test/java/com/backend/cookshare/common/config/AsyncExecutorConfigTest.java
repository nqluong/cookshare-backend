package com.backend.cookshare.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

class AsyncExecutorConfigTest {

    private final AsyncExecutorConfig config = new AsyncExecutorConfig();

    @Test
    void recipeLoaderExecutor_ShouldBeConfiguredProperly() {
        Executor executor = config.recipeLoaderExecutor();
        assertNotNull(executor);
        assertTrue(executor instanceof ThreadPoolTaskExecutor);

        ThreadPoolTaskExecutor tpe = (ThreadPoolTaskExecutor) executor;
        assertTrue(tpe.getCorePoolSize() > 0);
        assertTrue(tpe.getMaxPoolSize() >= tpe.getCorePoolSize());
        assertEquals(100, tpe.getQueueCapacity());
        assertEquals("recipe-loader-", tpe.getThreadNamePrefix());
    }

    @Test
    void reportAsyncExecutor_ShouldBeConfiguredProperly() {
        Executor executor = config.reportAsyncExecutor();
        assertNotNull(executor);
        assertTrue(executor instanceof ThreadPoolTaskExecutor);

        ThreadPoolTaskExecutor tpe = (ThreadPoolTaskExecutor) executor;
        assertTrue(tpe.getCorePoolSize() > 0);
        assertTrue(tpe.getMaxPoolSize() >= tpe.getCorePoolSize());
        assertEquals(200, tpe.getQueueCapacity());
        assertEquals("report-async-", tpe.getThreadNamePrefix());
    }

    @Test
    void statisticsExecutor_ShouldBeConfiguredProperly() {
        Executor executor = config.statisticsExecutor();
        assertNotNull(executor);
        assertTrue(executor instanceof ThreadPoolTaskExecutor);

        ThreadPoolTaskExecutor tpe = (ThreadPoolTaskExecutor) executor;
        assertTrue(tpe.getCorePoolSize() > 0);
        assertTrue(tpe.getMaxPoolSize() >= tpe.getCorePoolSize());
        assertEquals(150, tpe.getQueueCapacity());
        assertEquals("statistics-", tpe.getThreadNamePrefix());
    }

    @Test
    void taskExecutor_ShouldBeConfiguredProperly() {
        Executor executor = config.taskExecutor();
        assertNotNull(executor);
        assertTrue(executor instanceof ThreadPoolTaskExecutor);

        ThreadPoolTaskExecutor tpe = (ThreadPoolTaskExecutor) executor;
        assertTrue(tpe.getCorePoolSize() > 0);
        assertTrue(tpe.getMaxPoolSize() >= tpe.getCorePoolSize());
        assertEquals(500, tpe.getQueueCapacity());
        assertEquals("async-task-", tpe.getThreadNamePrefix());
    }
}
