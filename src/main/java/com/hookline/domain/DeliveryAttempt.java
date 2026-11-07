package com.hookline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One HTTP delivery attempt against an outbox event. This is the audit trail:
 * every try, its outcome, status code, latency, and error. The triage engine and
 * analytics (later weeks) read from here.
 */
@Entity
@Table(name = "delivery_attempts")
@Getter
public class DeliveryAttempt {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "outbox_event_id", nullable = false)
    private UUID outboxEventId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "response_status")
    private Integer responseStatus;

    private String error;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "verdict")
    private String verdict;

    @Column(name = "verdict_source")
    private String verdictSource;

    @CreationTimestamp
    @Column(name = "attempted_at", nullable = false, updatable = false)
    private OffsetDateTime attemptedAt;

    protected DeliveryAttempt() { }

    public DeliveryAttempt(UUID outboxEventId, int attemptNumber, boolean success,
                           Integer responseStatus, String error, long durationMs) {
        this.outboxEventId = outboxEventId;
        this.attemptNumber = attemptNumber;
        this.success = success;
        this.responseStatus = responseStatus;
        this.error = error;
        this.durationMs = durationMs;
    }

    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public void setVerdict(String verdict) { this.verdict = verdict; }

    public void setVerdictSource(String verdictSource) { this.verdictSource = verdictSource; }
}
