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
 * An inbound API key. We store ONLY a SHA-256 hash of the key (never the key
 * itself) - same reason you never store raw passwords: a DB leak can't be
 * replayed. Authentication hashes the presented key and looks up the hash.
 */
@Entity
@Table(name = "api_keys")
@Getter
public class ApiKey {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "key_hash", nullable = false)
    private String keyHash;   // hex-encoded SHA-256 (64 chars)

    @Column(nullable = false)
    private String label;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected ApiKey() { }

    public ApiKey(UUID tenantId, String keyHash, String label) {
        this.tenantId = tenantId;
        this.keyHash = keyHash;
        this.label = label;
    }
}
