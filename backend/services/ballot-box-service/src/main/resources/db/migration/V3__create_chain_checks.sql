-- RF-12, D-38: last verification of the hash chain of each process (who verified is not stored; the auditor's
-- actions are traced in RF-19). Problems as CODE@sequence, comma separated, at most 100.
CREATE TABLE chain_checks (
    process_id BIGINT PRIMARY KEY REFERENCES ballot_snapshots (process_id),
    checked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    intact BOOLEAN NOT NULL,
    events_checked INTEGER NOT NULL,
    final_hash VARCHAR(64),
    problems VARCHAR(4000) NOT NULL,
    CONSTRAINT ck_chain_checks_events CHECK (events_checked >= 0)
);
