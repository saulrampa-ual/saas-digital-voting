-- RF-19 (D-44): administrative actions waiting for identity-service, which keeps the traceability. A row is removed
-- once identity-service accepted it (idempotent by event_id).
CREATE TABLE admin_action_outbox (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    payload TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_admin_action_outbox_due ON admin_action_outbox (next_attempt_at, id);
