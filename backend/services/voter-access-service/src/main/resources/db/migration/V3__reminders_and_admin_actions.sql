-- RF-17 (D-45): reminders scheduled by the administrator; at their time, the people who have not voted yet receive a
-- new personal link. Only the number of recipients is kept here, never who.
CREATE TABLE reminders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    process_id BIGINT NOT NULL REFERENCES process_snapshots (process_id),
    send_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,
    recipients INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_reminders_status CHECK (status IN ('SCHEDULED', 'SENT', 'CANCELLED'))
);

CREATE INDEX ix_reminders_due ON reminders (send_at) WHERE status = 'SCHEDULED';

-- A reminder for someone who already voted, or once voting closed, is not sent and is not a failure either.
ALTER TABLE notifications DROP CONSTRAINT ck_notifications_status;
ALTER TABLE notifications ADD CONSTRAINT ck_notifications_status
    CHECK (status IN ('PENDING', 'DELIVERED', 'FAILED', 'CANCELLED'));

CREATE INDEX ix_notifications_process ON notifications (process_id, type, status);

-- RF-19 (D-44): administrative actions done here (reminders, retries) waiting for identity-service.
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
