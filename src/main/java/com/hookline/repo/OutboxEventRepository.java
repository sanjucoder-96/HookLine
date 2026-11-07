package com.hookline.repo;

import com.hookline.domain.OutboxEvent;
import com.hookline.domain.OutboxStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Claim a batch of due events. This is the heart of the poller.
     *
     * <p>{@code FOR UPDATE SKIP LOCKED} row-locks the selected events for the
     * current transaction and makes any OTHER poller instance transparently skip
     * rows already locked - so N pollers can drain the same table concurrently
     * without ever double-processing a row and without blocking each other.
     * The caller must mark the returned rows DISPATCHED and commit promptly, so
     * the locks are released before the (slow) HTTP delivery happens.
     */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING' AND next_attempt_at <= now()
            ORDER BY next_attempt_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> claimDue(@Param("batchSize") int batchSize);

    /**
     * Rescue orphans: rows left DISPATCHED by a worker that died mid-delivery.
     * Anything locked longer than the threshold goes back to PENDING to be retried.
     * This is what makes "at-least-once" hold across a crash.
     */
    @Transactional   // the scheduled reaper reaches this via an in-bean call that bypasses a
                     // service-level @Transactional proxy, so the tx boundary lives on the repo method
    @Modifying
    @Query(value = """
            UPDATE outbox_events
            SET status = 'PENDING', locked_at = NULL, updated_at = now()
            WHERE status = 'DISPATCHED' AND locked_at < :threshold
            """, nativeQuery = true)
    int reapStuck(@Param("threshold") OffsetDateTime threshold);

    List<OutboxEvent> findByStatusOrderByCreatedAtDesc(OutboxStatus status);

    List<OutboxEvent> findTop100ByOrderByCreatedAtDesc();

    long countByStatus(OutboxStatus status);
}
