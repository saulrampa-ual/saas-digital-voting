-- RF-03, RI-3: censuses and voters of each organization
CREATE TABLE censuses (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organization_id VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_censuses_name CHECK (length(trim(name)) BETWEEN 1 AND 100)
);

CREATE INDEX ix_censuses_organization ON censuses (organization_id);

CREATE TABLE voters (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organization_id VARCHAR(20) NOT NULL,
    census_id BIGINT NOT NULL REFERENCES censuses (id),
    full_name VARCHAR(60) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(15),
    institutional_id VARCHAR(50),
    preferred_language VARCHAR(2) NOT NULL DEFAULT 'es',
    anonymized_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_voters_full_name CHECK (length(trim(full_name)) BETWEEN 1 AND 60),
    CONSTRAINT ck_voters_phone CHECK (phone IS NULL OR phone ~ '^[0-9]{1,15}$'),
    CONSTRAINT ck_voters_language CHECK (preferred_language IN ('es', 'en'))
);

-- Duplicates are detected per census by email (case-insensitive) or institutional identifier (RF-03)
CREATE UNIQUE INDEX ux_voters_census_email ON voters (census_id, lower(email));
CREATE UNIQUE INDEX ux_voters_census_institutional_id ON voters (census_id, institutional_id)
    WHERE institutional_id IS NOT NULL;
CREATE INDEX ix_voters_organization ON voters (organization_id);
