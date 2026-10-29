CREATE TABLE tenants (
    identifier VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_tenants_identifier_format
        CHECK (identifier ~ '^[A-Za-z0-9]{1,20}$'),
    CONSTRAINT ck_tenants_name_not_blank
        CHECK (length(trim(name)) BETWEEN 1 AND 100),
    CONSTRAINT ck_tenants_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);
