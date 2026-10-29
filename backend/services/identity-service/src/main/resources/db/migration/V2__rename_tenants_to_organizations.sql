-- RF-01: the TFM and the API use the term "organization" (D-23)
ALTER TABLE tenants RENAME TO organizations;
ALTER TABLE organizations RENAME COLUMN identifier TO id;
ALTER TABLE organizations RENAME CONSTRAINT tenants_pkey TO organizations_pkey;
ALTER TABLE organizations RENAME CONSTRAINT ck_tenants_identifier_format TO ck_organizations_id_format;
ALTER TABLE organizations RENAME CONSTRAINT ck_tenants_name_not_blank TO ck_organizations_name_not_blank;
ALTER TABLE organizations RENAME CONSTRAINT ck_tenants_status TO ck_organizations_status;
