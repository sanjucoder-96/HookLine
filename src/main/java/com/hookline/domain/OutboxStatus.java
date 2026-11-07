package com.hookline.domain;

/**
 * Lifecycle of an outbox event.
 * <pre>
 *   PENDING ──claim──▶ DISPATCHED ──2xx──▶ DELIVERED   (terminal)
 *      ▲                   │
 *      │ retry / reaper    └──failure, attempts left──▶ PENDING
 *      │                   └──failure, out of attempts─▶ DEAD  (terminal, the DLQ)
 * </pre>
 */
public enum OutboxStatus {
    PENDING,     // due for delivery (a new event, or a retry released from the delay queue)
    SCHEDULED,   // parked in the Redis delay queue, waiting for its retry time
    DISPATCHED,  // claimed by a poller, delivery in flight (the reaper rescues it if the worker dies)
    DELIVERED,   // terminal: a 2xx was received
    DEAD         // terminal: gave up after max attempts -> the dead-letter queue
}
