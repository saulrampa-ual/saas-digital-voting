-- RF-06, RF-07, RF-20, RI-4, RI-5: voting processes, their ballots and translations (D-29)
CREATE TABLE processes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organization_id VARCHAR(20) NOT NULL,
    census_id BIGINT NOT NULL REFERENCES censuses (id),
    opens_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closes_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    default_language VARCHAR(2) NOT NULL,
    results_visibility VARCHAR(20) NOT NULL DEFAULT 'CENSUS_ONLY',
    results_public_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_processes_dates CHECK (closes_at > opens_at),
    CONSTRAINT ck_processes_status CHECK (status IN ('DRAFT', 'SCHEDULED', 'OPEN', 'CLOSED', 'TALLIED', 'ARCHIVED')),
    CONSTRAINT ck_processes_language CHECK (default_language IN ('es', 'en')),
    CONSTRAINT ck_processes_visibility CHECK (results_visibility IN ('CENSUS_ONLY', 'PUBLIC_LINK'))
);

CREATE INDEX ix_processes_organization ON processes (organization_id);
-- Census freeze: is there a SCHEDULED or OPEN process using this census? (RF-03, D-21)
CREATE INDEX ix_processes_census_status ON processes (census_id, status);

-- Title and description per language; the default language is always present (checked by the service)
CREATE TABLE process_texts (
    process_id BIGINT NOT NULL REFERENCES processes (id) ON DELETE CASCADE,
    language VARCHAR(2) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    PRIMARY KEY (process_id, language),
    CONSTRAINT ck_process_texts_language CHECK (language IN ('es', 'en')),
    CONSTRAINT ck_process_texts_title CHECK (length(trim(title)) BETWEEN 1 AND 150)
);

CREATE TABLE ballots (
    process_id BIGINT PRIMARY KEY REFERENCES processes (id) ON DELETE CASCADE,
    vote_type VARCHAR(20) NOT NULL,
    blank_vote_enabled BOOLEAN NOT NULL,
    CONSTRAINT ck_ballots_vote_type CHECK (vote_type IN ('SINGLE_CHOICE', 'YES_NO_ABSTAIN'))
);

CREATE TABLE ballot_texts (
    process_id BIGINT NOT NULL REFERENCES ballots (process_id) ON DELETE CASCADE,
    language VARCHAR(2) NOT NULL,
    question VARCHAR(300) NOT NULL,
    PRIMARY KEY (process_id, language),
    CONSTRAINT ck_ballot_texts_language CHECK (language IN ('es', 'en'))
);

CREATE TABLE ballot_options (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    process_id BIGINT NOT NULL REFERENCES ballots (process_id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    CONSTRAINT ck_ballot_options_position CHECK (position >= 1),
    -- Deferred: replacing the options of a draft inserts the new rows before deleting the old ones
    CONSTRAINT ux_ballot_options_position UNIQUE (process_id, position) DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE ballot_option_texts (
    option_id BIGINT NOT NULL REFERENCES ballot_options (id) ON DELETE CASCADE,
    language VARCHAR(2) NOT NULL,
    text VARCHAR(200) NOT NULL,
    PRIMARY KEY (option_id, language),
    CONSTRAINT ck_ballot_option_texts_language CHECK (language IN ('es', 'en'))
);

-- RF-07, RF-19: who changed the status and when (SYSTEM for automatic transitions)
CREATE TABLE process_status_changes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    process_id BIGINT NOT NULL REFERENCES processes (id),
    from_status VARCHAR(20) NOT NULL,
    to_status VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    changed_by VARCHAR(36) NOT NULL
);

CREATE INDEX ix_process_status_changes_process ON process_status_changes (process_id, changed_at);
