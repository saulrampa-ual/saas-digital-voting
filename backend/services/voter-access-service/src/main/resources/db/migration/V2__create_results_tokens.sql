-- RF-15 with CENSUS_ONLY (D-41): after the tally, the personal link and a code give a token to see the result.
-- Only its hash; no roll entry, so the token says that someone of the census is looking, never who.
CREATE TABLE results_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    process_id BIGINT NOT NULL REFERENCES process_snapshots (process_id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_results_tokens_process ON results_tokens (process_id);
