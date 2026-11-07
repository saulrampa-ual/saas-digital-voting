-- RF-16, RI-10, D-40: title of the process (only for the minutes; not part of the canonical ballot of OPENED) and the
-- minutes in PDF, generated the first time they are asked for in each language and then always served the same.
CREATE TABLE ballot_titles (
    process_id BIGINT NOT NULL REFERENCES ballot_snapshots (process_id) ON DELETE CASCADE,
    language VARCHAR(2) NOT NULL,
    title VARCHAR(150) NOT NULL,
    PRIMARY KEY (process_id, language),
    CONSTRAINT ck_ballot_titles_language CHECK (language IN ('es', 'en'))
);

CREATE TABLE minutes (
    process_id BIGINT NOT NULL REFERENCES results (process_id),
    language VARCHAR(2) NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    pdf BYTEA NOT NULL,
    PRIMARY KEY (process_id, language),
    CONSTRAINT ck_minutes_language CHECK (language IN ('es', 'en'))
);
