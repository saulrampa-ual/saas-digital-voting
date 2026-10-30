-- RF-13, RF-15, RI-10 (D-37): result of the automatic tally. Only counts and hashes: no identity, no receipt and no
-- link to a vote. Percentages are derived from the counts when read (2 decimals).
CREATE TABLE results (
    process_id BIGINT PRIMARY KEY REFERENCES ballot_snapshots (process_id),
    tallied_at TIMESTAMP WITH TIME ZONE NOT NULL,
    eligible_voters INTEGER NOT NULL,
    total_votes INTEGER NOT NULL,
    blank_votes INTEGER NOT NULL,
    votes_hash VARCHAR(64) NOT NULL,
    result_hash VARCHAR(64) NOT NULL,
    -- election-service acknowledged the tally (TALLIED); retried until true
    election_notified BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_results_counts CHECK (total_votes >= 0 AND blank_votes BETWEEN 0 AND total_votes),
    CONSTRAINT ck_results_eligible CHECK (eligible_voters >= 1)
);

CREATE TABLE result_options (
    process_id BIGINT NOT NULL REFERENCES results (process_id) ON DELETE CASCADE,
    option_id BIGINT NOT NULL REFERENCES ballot_options (id),
    votes INTEGER NOT NULL,
    PRIMARY KEY (process_id, option_id),
    CONSTRAINT ck_result_options_votes CHECK (votes >= 0)
);

-- RF-13 exception 5: tally not published because it could not be trusted
ALTER TABLE incidents DROP CONSTRAINT ck_incidents_kind;
ALTER TABLE incidents ADD CONSTRAINT ck_incidents_kind CHECK (kind IN ('REVERT_FAILED', 'CHAIN_BROKEN', 'TALLY_FAILED'));
