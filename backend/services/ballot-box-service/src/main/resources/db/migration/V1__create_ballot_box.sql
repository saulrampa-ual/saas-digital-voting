-- RF-09, RF-10, RF-12, RI-5, RI-6, RI-8, RI-9 (ADR-0005, ADR-0006, ADR-0008, D-18, D-20).
-- The ballot box stores no identity, no voting token and no time of votes (RNF-11).
CREATE TABLE ballot_snapshots (
    process_id BIGINT PRIMARY KEY,
    organization_id VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    opens_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closes_at TIMESTAMP WITH TIME ZONE NOT NULL,
    vote_type VARCHAR(20) NOT NULL,
    blank_vote_enabled BOOLEAN NOT NULL,
    eligible_voters INTEGER NOT NULL,
    results_visibility VARCHAR(20) NOT NULL,
    results_public_id UUID,
    final_hash VARCHAR(64),
    -- Head of the hash chain; the row is locked to append events in order (data-model, section 5)
    last_sequence BIGINT NOT NULL DEFAULT 0,
    last_event_hash VARCHAR(64) NOT NULL DEFAULT repeat('0', 64),
    CONSTRAINT ck_ballot_snapshots_status CHECK (status IN ('SCHEDULED', 'OPEN', 'CLOSED', 'TALLIED', 'ARCHIVED')),
    CONSTRAINT ck_ballot_snapshots_dates CHECK (closes_at > opens_at),
    CONSTRAINT ck_ballot_snapshots_vote_type CHECK (vote_type IN ('SINGLE_CHOICE', 'YES_NO_ABSTAIN')),
    CONSTRAINT ck_ballot_snapshots_visibility CHECK (results_visibility IN ('CENSUS_ONLY', 'PUBLIC_LINK')),
    CONSTRAINT ck_ballot_snapshots_eligible CHECK (eligible_voters >= 1)
);

CREATE TABLE ballot_questions (
    process_id BIGINT NOT NULL REFERENCES ballot_snapshots (process_id) ON DELETE CASCADE,
    language VARCHAR(2) NOT NULL,
    question VARCHAR(300) NOT NULL,
    PRIMARY KEY (process_id, language),
    CONSTRAINT ck_ballot_questions_language CHECK (language IN ('es', 'en'))
);

-- Identifiers assigned by election-service
CREATE TABLE ballot_options (
    id BIGINT PRIMARY KEY,
    process_id BIGINT NOT NULL REFERENCES ballot_snapshots (process_id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    CONSTRAINT ux_ballot_options_position UNIQUE (process_id, position)
);

CREATE TABLE ballot_option_texts (
    option_id BIGINT NOT NULL REFERENCES ballot_options (id) ON DELETE CASCADE,
    language VARCHAR(2) NOT NULL,
    text VARCHAR(200) NOT NULL,
    PRIMARY KEY (option_id, language),
    CONSTRAINT ck_ballot_option_texts_language CHECK (language IN ('es', 'en'))
);

-- RI-6 with D-18: random id, the option (NULL = blank) and nothing else
CREATE TABLE votes (
    id UUID PRIMARY KEY,
    process_id BIGINT NOT NULL REFERENCES ballot_snapshots (process_id),
    option_id BIGINT REFERENCES ballot_options (id),
    status VARCHAR(20) NOT NULL DEFAULT 'COUNTABLE',
    CONSTRAINT ck_votes_status CHECK (status IN ('COUNTABLE', 'SUBSTITUTED'))
);

CREATE INDEX ix_votes_process ON votes (process_id);

-- RI-8 with D-18: only the hash of the receipt code, no time, no link to the vote
CREATE TABLE receipts (
    process_id BIGINT NOT NULL REFERENCES ballot_snapshots (process_id),
    receipt_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VALID',
    PRIMARY KEY (process_id, receipt_hash),
    CONSTRAINT ck_receipts_status CHECK (status IN ('VALID', 'SUBSTITUTED'))
);

-- RI-9, ADR-0006: hash chain; only process events carry a time (D-18)
CREATE TABLE audit_events (
    process_id BIGINT NOT NULL REFERENCES ballot_snapshots (process_id),
    sequence BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE,
    payload_hash VARCHAR(64) NOT NULL,
    previous_hash VARCHAR(64) NOT NULL,
    event_hash VARCHAR(64) NOT NULL,
    PRIMARY KEY (process_id, sequence),
    CONSTRAINT ck_audit_events_type CHECK (event_type IN ('OPENED', 'VOTE_RECEIVED', 'CLOSED', 'FINAL_HASH_PUBLISHED')),
    CONSTRAINT ck_audit_events_time CHECK ((event_type = 'VOTE_RECEIVED') = (occurred_at IS NULL))
);

-- ADR-0005: vote not stored and consumption not reverted; only the day (D-18), never the token
CREATE TABLE incidents (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    process_id BIGINT NOT NULL REFERENCES ballot_snapshots (process_id),
    kind VARCHAR(30) NOT NULL,
    occurred_on DATE NOT NULL,
    CONSTRAINT ck_incidents_kind CHECK (kind IN ('REVERT_FAILED', 'CHAIN_BROKEN'))
);
