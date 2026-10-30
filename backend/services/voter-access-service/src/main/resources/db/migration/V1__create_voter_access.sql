-- RF-08, RI-3, RI-7, RI-11, RI-12, D-18, D-21: processes received from election, frozen roll, access credentials,
-- participation, single-use voting tokens and notifications.
CREATE TABLE process_snapshots (
    process_id BIGINT PRIMARY KEY,
    organization_id VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    opens_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closes_at TIMESTAMP WITH TIME ZONE NOT NULL,
    default_language VARCHAR(2) NOT NULL,
    results_visibility VARCHAR(20) NOT NULL,
    CONSTRAINT ck_process_snapshots_status CHECK (status IN ('SCHEDULED', 'OPEN', 'CLOSED', 'TALLIED', 'ARCHIVED')),
    CONSTRAINT ck_process_snapshots_dates CHECK (closes_at > opens_at),
    CONSTRAINT ck_process_snapshots_language CHECK (default_language IN ('es', 'en')),
    CONSTRAINT ck_process_snapshots_visibility CHECK (results_visibility IN ('CENSUS_ONLY', 'PUBLIC_LINK'))
);

CREATE TABLE process_snapshot_texts (
    process_id BIGINT NOT NULL REFERENCES process_snapshots (process_id) ON DELETE CASCADE,
    language VARCHAR(2) NOT NULL,
    title VARCHAR(150) NOT NULL,
    PRIMARY KEY (process_id, language),
    CONSTRAINT ck_process_snapshot_texts_language CHECK (language IN ('es', 'en'))
);

-- Only the data needed to contact the voter (RNF-16); random identifier
CREATE TABLE roll_entries (
    id UUID PRIMARY KEY,
    process_id BIGINT NOT NULL REFERENCES process_snapshots (process_id),
    voter_ref BIGINT NOT NULL,
    display_name VARCHAR(60) NOT NULL,
    email VARCHAR(100) NOT NULL,
    language VARCHAR(2) NOT NULL,
    CONSTRAINT ux_roll_entries_voter UNIQUE (process_id, voter_ref),
    CONSTRAINT ck_roll_entries_language CHECK (language IN ('es', 'en'))
);

-- RI-12: only hashes of the personal link and of the code
CREATE TABLE access_credentials (
    roll_entry_id UUID PRIMARY KEY REFERENCES roll_entries (id),
    link_hash VARCHAR(64) UNIQUE,
    link_expires_at TIMESTAMP WITH TIME ZONE,
    otp_hash VARCHAR(64),
    otp_expires_at TIMESTAMP WITH TIME ZONE,
    otp_sent_at TIMESTAMP WITH TIME ZONE,
    otp_window_started_at TIMESTAMP WITH TIME ZONE,
    otp_sent_in_window SMALLINT NOT NULL DEFAULT 0,
    failed_attempts SMALLINT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE
);

-- RI-7, D-18: whether the voter has voted and the day, never the time
CREATE TABLE participation (
    roll_entry_id UUID PRIMARY KEY REFERENCES roll_entries (id),
    has_voted BOOLEAN NOT NULL DEFAULT FALSE,
    voted_on DATE,
    CONSTRAINT ck_participation_day CHECK (has_voted = (voted_on IS NOT NULL))
);

-- D-20: single-use voting tokens, stored as hashes
CREATE TABLE voting_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    roll_entry_id UUID NOT NULL REFERENCES roll_entries (id),
    process_id BIGINT NOT NULL REFERENCES process_snapshots (process_id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT ck_voting_tokens_status CHECK (status IN ('ISSUED', 'CONSUMED', 'REVERTED', 'REVOKED'))
);

CREATE INDEX ix_voting_tokens_entry ON voting_tokens (roll_entry_id, status);

-- RI-11: invitations now; reminders and closing notices with RF-17 (P6). Never contain the chosen option.
CREATE SEQUENCE notifications_seq INCREMENT BY 50;

CREATE TABLE notifications (
    id BIGINT PRIMARY KEY,
    process_id BIGINT NOT NULL REFERENCES process_snapshots (process_id),
    roll_entry_id UUID NOT NULL REFERENCES roll_entries (id),
    type VARCHAR(20) NOT NULL,
    channel VARCHAR(10) NOT NULL DEFAULT 'EMAIL',
    status VARCHAR(20) NOT NULL,
    attempts SMALLINT NOT NULL DEFAULT 0,
    last_error VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_notifications_type CHECK (type IN ('INVITATION', 'REMINDER', 'CLOSING')),
    CONSTRAINT ck_notifications_channel CHECK (channel IN ('EMAIL')),
    CONSTRAINT ck_notifications_status CHECK (status IN ('PENDING', 'DELIVERED', 'FAILED')),
    CONSTRAINT ck_notifications_attempts CHECK (attempts BETWEEN 0 AND 9)
);

CREATE INDEX ix_notifications_due ON notifications (next_attempt_at) WHERE status = 'PENDING';
