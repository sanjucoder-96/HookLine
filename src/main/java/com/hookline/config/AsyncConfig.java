package com.hookline.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread pools for the delivery spine.
 *
 * <p><b>deliveryExecutor</b> lets one poll tick deliver its whole claimed batch
 * concurrently. This is not a nicety: a claimed row sits in DISPATCHED until its
 * outcome is recorded, and the reaper reclaims DISPATCHED rows older than
 * {@code dispatch-timeout-ms}. If we delivered a batch of 20 sequentially with a
 * 5s read timeout, the batch could outlive the timeout and get falsely reaped
 * (=> double delivery). Concurrent dispatch keeps a batch's wall-clock ~= one
 * delivery, comfortably under the timeout.
 *
 * <p><b>taskScheduler</b> is sized 2 so the poller and the reaper run on
 * independent threads (the default Spring scheduler is single-threaded, which
 * would make the reaper wait behind an in-progress poll).
 */
@Configuration
public class AsyncConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService deliveryExecutor(HooklineProperties props) {
        int n = Math.max(1, props.getDelivery().getBatchSize());
        AtomicInteger seq = new AtomicInteger();
        return Executors.newFixedThreadPool(n, r -> {
            Thread t = new Thread(r, "delivery-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler s = new ThreadPoolTaskScheduler();
        s.setPoolSize(4);   // poller, reaper, delay-queue poll + reconcile each need a thread
        s.setThreadNamePrefix("hookline-sched-");
        s.setDaemon(true);
        s.initialize();
        return s;
    }
}
