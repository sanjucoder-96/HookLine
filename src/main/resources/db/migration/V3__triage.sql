-- V3: triage. Persist the response BODY (the triage engine and audit trail need it
-- to classify a lying-200 or an HTML 502) and the verdict each failed attempt was
-- classified as, plus which tier produced it (for a rules-vs-LLM eval later).
alter table delivery_attempts add column response_body  text;
alter table delivery_attempts add column verdict        text;   -- TRANSIENT_DOWN | RATE_LIMITED | INVALID_AUTH | ENDPOINT_GONE | INVALID_PAYLOAD
alter table delivery_attempts add column verdict_source text;   -- RULES | LLM | CACHE | FALLBACK
