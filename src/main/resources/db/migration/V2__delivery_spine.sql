-- V2: the delivery spine.
--
-- orders            - a demo BUSINESS table. It exists so we can show the
--                     transactional outbox for real: an order row and its webhook
--                     outbox row are written in ONE transaction (see OrderService).
-- outbox_events     - the transactional outbox AND the unit the poller drives to a
--                     terminal state (DELIVERED or DEAD).
-- delivery_attempts - one row per HTTP attempt: the audit trail the triage engine
--                     and analytics build on later.

create table orders (
    id            uuid        primary key default gen_random_uuid(),
    tenant_id     uuid        not null references tenants(id),
    customer_name text        not null,
    amount_cents  bigint      not null check (amount_cents >= 0),
    status        text        not null default 'CREATED',
    created_at    timestamptz not null default now()
);
create index idx_orders_tenant on orders (tenant_id);

create table outbox_events (
    id               uuid        primary key default gen_random_uuid(),
    tenant_id        uuid        not null references tenants(id),
    endpoint_id      uuid        not null references endpoints(id),
    event_type       text        not null,
    payload          jsonb       not null,
    idempotency_key  uuid        not null,          -- stable across retries; receiver dedupes on it
    status           text        not null default 'PENDING',  -- PENDING|DISPATCHED|DELIVERED|DEAD
    attempt_count    int         not null default 0,
    next_attempt_at  timestamptz not null default now(),
    locked_at        timestamptz,                   -- when a poller claimed it (drives the reaper)
    last_status_code int,
    last_error       text,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now()
);
-- The poller's hot path: pending & due, oldest first. A PARTIAL index keeps it
-- tiny - it only holds rows actually awaiting delivery, not the delivered history.
create index idx_outbox_due on outbox_events (next_attempt_at) where status = 'PENDING';
-- The reaper's query: DISPATCHED rows whose worker may have died.
create index idx_outbox_dispatched on outbox_events (locked_at) where status = 'DISPATCHED';

create table delivery_attempts (
    id              uuid        primary key default gen_random_uuid(),
    outbox_event_id uuid        not null references outbox_events(id) on delete cascade,
    attempt_number  int         not null,
    success         boolean     not null,
    response_status int,
    error           text,
    duration_ms     bigint      not null,
    attempted_at    timestamptz not null default now()
);
create index idx_attempts_event on delivery_attempts (outbox_event_id);
