-- RF-21 (D-47): keys of each organization for its integrations. Each one is a confidential Keycloak client
-- (client credentials, role INTEGRATION, claim organization_id); the secret lives only in Keycloak.
CREATE TABLE api_keys (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organization_id VARCHAR(20) NOT NULL REFERENCES organizations (id),
    name VARCHAR(60) NOT NULL,
    client_id VARCHAR(60) NOT NULL UNIQUE,
    keycloak_id UUID NOT NULL,
    created_by UUID NOT NULL,
    created_by_name VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_api_keys_name CHECK (length(trim(name)) BETWEEN 1 AND 60)
);

CREATE INDEX ix_api_keys_organization ON api_keys (organization_id, created_at DESC);
