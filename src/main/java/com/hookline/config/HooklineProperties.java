package com.hookline.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for the delivery engine, bound from the {@code hookline.*}
 * keys in application.yml. Everything the poller/delivery/backoff needs is here,
 * so there are no magic numbers scattered through the code.
 */
@ConfigurationProperties(prefix = "hookline")
@Getter
public class HooklineProperties {

    private final Delivery delivery = new Delivery();
    private final Dev dev = new Dev();
    private final Triage triage = new Triage();
    private final CircuitBreakerSettings circuitBreaker = new CircuitBreakerSettings();

    @Getter
    @Setter
    public static class Delivery {
        private long pollIntervalMs = 2000;
        private long reaperIntervalMs = 30000;
        private long dispatchTimeoutMs = 60000;  // a DISPATCHED row older than this is reaped
        private int batchSize = 20;
        private int maxAttempts = 5;
        private final Backoff backoff = new Backoff();
        private final Http http = new Http();
    }

    @Getter
    @Setter
    public static class Backoff {
        private long baseMs = 3000;
        private long capMs = 30000;
    }

    @Getter
    @Setter
    public static class Http {
        private int connectTimeoutMs = 2000;
        private int readTimeoutMs = 5000;
    }

    @Getter
    @Setter
    public static class Dev {
        private boolean seed = true;
        private String sampleEndpointBase = "http://127.0.0.1:9000";  // the chaos fleet
    }

    @Getter
    @Setter
    public static class Triage {
        private boolean enabled = true;
        private double confidenceThreshold = 0.6;   // below this, fall back to a safe retry
        private final Gemini gemini = new Gemini();
    }

    @Getter
    @Setter
    public static class Gemini {
        private String apiKey = "";                 // blank => LLM tier disabled (rules only)
        private String model = "gemini-2.0-flash";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        private long timeoutMs = 10000;
    }

    @Getter
    @Setter
    public static class CircuitBreakerSettings {
        private float failureRateThreshold = 50;        // % of the window that must fail to open
        private int slidingWindowSize = 10;
        private int minimumNumberOfCalls = 5;           // don't evaluate until this many calls recorded
        private long waitDurationInOpenSeconds = 30;    // stay open this long, then probe (half-open)
        private int permittedCallsInHalfOpen = 2;
        private long openDeferMs = 5000;                // when open, push a skipped event's next attempt out by this
    }
}
