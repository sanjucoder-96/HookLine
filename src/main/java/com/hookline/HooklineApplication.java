package com.hookline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Hookline - webhook delivery platform with AI failure triage.
 *
 * <p>{@code @EnableScheduling} drives the two background heartbeats of the
 * delivery spine: the outbox poller (drains {@code outbox_events}) and the
 * reaper (returns rows stuck in DISPATCHED - a worker that died mid-delivery -
 * back to PENDING). {@code HooklineProperties} is registered via
 * {@code @EnableConfigurationProperties} in {@link com.hookline.config.RestClientConfig}.
 */
@SpringBootApplication
@EnableScheduling
public class HooklineApplication {

    public static void main(String[] args) {
        SpringApplication.run(HooklineApplication.class, args);
    }
}
