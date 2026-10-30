-- D-21, ADR-0002: outbox of process changes for voter-access and ballot-box, written in the same transaction as
-- the status change and delivered in order (per process and target) by a scheduled task until confirmed.
CREATE TABLE process_sync (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    process_id BIGINT NOT NULL REFERENCES processes (id),
    target VARCHAR(20) NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    -- JSON body of the internal call. Cleared once delivered: the roll carries personal data (RNF-16).
    payload TEXT,
    state VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    delivered_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_process_sync_target CHECK (target IN ('VOTER_ACCESS', 'BALLOT_BOX')),
    CONSTRAINT ck_process_sync_type CHECK (message_type IN ('SNAPSHOT', 'STATUS')),
    CONSTRAINT ck_process_sync_state CHECK (state IN ('PENDING', 'DELIVERED', 'FAILED')),
    CONSTRAINT ck_process_sync_payload CHECK (state = 'DELIVERED' OR payload IS NOT NULL)
);

CREATE INDEX ix_process_sync_pending ON process_sync (id) WHERE state = 'PENDING';
