-- RF-02, RI-2: local copy of management users. Credentials, second factor and roles live in Keycloak (D-02).
CREATE TABLE management_users (
    id UUID PRIMARY KEY,
    organization_id VARCHAR(20) NOT NULL REFERENCES organizations (id),
    full_name VARCHAR(60) NOT NULL,
    email VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_management_users_full_name CHECK (length(trim(full_name)) BETWEEN 1 AND 60),
    CONSTRAINT ck_management_users_role CHECK (role IN ('ADMIN', 'CENSUS_MANAGER', 'AUDITOR'))
);

CREATE UNIQUE INDEX ux_management_users_email ON management_users (lower(email));
CREATE INDEX ix_management_users_organization_role ON management_users (organization_id, role);
