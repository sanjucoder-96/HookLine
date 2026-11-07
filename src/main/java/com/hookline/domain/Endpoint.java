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
 * A registered delivery destination. {@code secret} is the HMAC key we sign each
 * payload with so the receiver can verify authenticity.
 *
 * <p>Note the foreign keys ({@code tenantId}) are plain UUID columns, not JPA
 * {@code @ManyToOne} associations. This service moves IDs and payloads around; it
 * never needs to navigate object graphs, so we avoid lazy-loading traps entirely.
 */
@Entity
@Table(name = "endpoints")
@Getter
@Setter
public class Endpoint {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String secret;

    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Endpoint() { }

    public Endpoint(UUID tenantId, String url, String secret, String description) {
        this.tenantId = tenantId;
        this.url = url;
        this.secret = secret;
        this.description = description;
    }
}
