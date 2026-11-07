package com.hookline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The transactional-outbox row AND the unit the poller drives to a terminal
 * state. The state-transition rules live here (a rich domain model) so they sit
 * in one place and the services stay thin. There are no public setters - callers
 * move the row through its lifecycle via the intent methods below.
 */
@Entity
@Table(name = "outbox_events")
@Getter
public class OutboxEvent {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "endpoint_id", nullable = false)
    private UUID endpointId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    // Stored as jsonb; we keep it as a String in Java (we never query inside it here).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    // Stable across every retry - the receiver dedupes on this key.
    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt = OffsetDateTime.now();

    @Column(name = "locked_at")
    private OffsetDateTime lockedAt;

    @Column(name = "last_status_code")
    private Integer lastStatusCode;

    @Column(name = "last_error")
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected OutboxEvent() { }

    public OutboxEvent(UUID tenantId, UUID endpointId, String eventType, String payload) {
        this.tenantId = tenantId;
        this.endpointId = endpointId;
        this.eventType = eventType;
        this.payload = payload;
    }

    /** The attempt number this row is about to make (1-based). */
    public int nextAttemptNumber() {
        return attemptCount + 1;
    }

    /** A poller claimed this row: mark it in-flight and stamp the lock time for the reaper. */
    public void markDispatched(OffsetDateTime now) {
        this.status = OutboxStatus.DISPATCHED;
        this.lockedAt = now;
    }

    /** A 2xx came back. Terminal. */
    public void markDelivered(int statusCode) {
        this.attemptCount++;
        this.status = OutboxStatus.DELIVERED;
        this.lastStatusCode = statusCode;
        this.lastError = null;
        this.lockedAt = null;
    }

    /** The attempt failed, but attempts remain: go back to PENDING, due at {@code nextAttemptAt}. */
    public void scheduleRetry(OffsetDateTime nextAttemptAt, Integer statusCode, String error) {
        this.attemptCount++;
        this.status = OutboxStatus.PENDING;
        this.nextAttemptAt = nextAttemptAt;
        this.lastStatusCode = statusCode;
        this.lastError = error;
        this.lockedAt = null;
    }

    /**
     * The endpoint's circuit is open: put the row back to PENDING for later WITHOUT
     * consuming an attempt - nothing was actually delivered, so it must not count
     * toward max-attempts.
     */
    public void deferDelivery(OffsetDateTime nextAttemptAt) {
        this.status = OutboxStatus.PENDING;
        this.nextAttemptAt = nextAttemptAt;
        this.lockedAt = null;
    }

    /** Failed but retries remain: park it in the Redis delay queue, due at {@code dueAt}. */
    public void scheduleInDelayQueue(OffsetDateTime dueAt, Integer statusCode, String error) {
        this.attemptCount++;
        this.status = OutboxStatus.SCHEDULED;
        this.nextAttemptAt = dueAt;
        this.lastStatusCode = statusCode;
        this.lastError = error;
        this.lockedAt = null;
    }

    /** The delay-queue worker found this event due: make it claimable by the poller again. */
    public void releaseFromDelayQueue(OffsetDateTime now) {
        this.status = OutboxStatus.PENDING;
        this.nextAttemptAt = now;
        this.lockedAt = null;
    }

    /** Out of attempts. Terminal -> the DLQ. */
    public void markDead(Integer statusCode, String error) {
        this.attemptCount++;
        this.status = OutboxStatus.DEAD;
        this.lastStatusCode = statusCode;
        this.lastError = error;
        this.lockedAt = null;
    }
}
