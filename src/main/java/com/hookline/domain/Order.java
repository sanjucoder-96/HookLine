package com.hookline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A demo business record. Its only job is to make the transactional outbox
 * concrete: creating an order and enqueuing its "order.created" webhook happen
 * in a single DB transaction (see OrderService).
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(nullable = false)
    private String status = "CREATED";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Order() { }

    public Order(UUID tenantId, String customerName, long amountCents) {
        this.tenantId = tenantId;
        this.customerName = customerName;
        this.amountCents = amountCents;
    }
}
