-- V1: multi-tenancy + endpoint registry (the foundation).
--
-- The delivery-semantics tables (outbox_events, delivery, delivery_attempt,
-- dead_letter) are intentionally NOT here yet. Their columns encode the
-- at-least-once / idempotency contract from blueprint.md section 5, so we
-- design them together when we build the delivery spine in week 2.

create extension if not exists "pgcrypto";      -- provides gen_random_uuid()

-- A customer of the platform. Every other row is scoped to exactly one tenant.
create table tenants (
    id          uuid        primary key default gen_random_uuid(),
    name        text        not null,
    created_at  timestamptz not null default now()
);

-- Inbound API keys. We store ONLY a SHA-256 hash of the key, never the key
-- itself - the same reason you never store raw passwords. Verification hashes
-- the presented key and compares.
create table api_keys (
    id          uuid        primary key default gen_random_uuid(),
    tenant_id   uuid        not null references tenants(id) on delete cascade,
    key_hash    char(64)    not null unique,        -- hex-encoded SHA-256
    label       text        not null,
    created_at  timestamptz not null default now(),
    revoked_at  timestamptz
);
create index idx_api_keys_tenant on api_keys (tenant_id);

-- A registered delivery destination. `secret` is used to HMAC-sign each payload
-- so the receiver can verify authenticity (blueprint.md section 5).
create table endpoints (
    id          uuid        primary key default gen_random_uuid(),
    tenant_id   uuid        not null references tenants(id) on delete cascade,
    url         text        not null,
    secret      text        not null,
    description text,
    active      boolean     not null default true,
    created_at  timestamptz not null default now()
);
create index idx_endpoints_tenant on endpoints (tenant_id);
