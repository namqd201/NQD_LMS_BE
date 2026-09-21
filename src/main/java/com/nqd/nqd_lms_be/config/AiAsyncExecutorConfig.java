package com.nqd.nqd_lms_be.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
@Slf4j
public class AiAsyncExecutorConfig {

    /**
     * Dedicated Virtual Thread Task Executor for AI Provider Calls (Gemini, Groq, OpenAI).
     * Prevents heavy LLM inference latency from blocking Tomcat worker threads or holding DB connections.
     */
    @Bean(name = "aiTaskExecutor")
    public AsyncTaskExecutor aiTaskExecutor() {
        log.info("Initializing Virtual Thread Task Executor for AI Provider calls (JDK 21 Loom)...");
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("ai-vthread-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(10000);
        return executor;
    }

    /**
     * General Virtual Thread Task Executor for background notifications, email, and audit logs.
     */
    @Bean(name = "applicationTaskExecutor")
    public Executor applicationTaskExecutor() {
        log.info("Initializing Application Virtual Thread Executor...");
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
