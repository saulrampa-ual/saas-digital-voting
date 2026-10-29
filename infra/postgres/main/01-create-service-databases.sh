#!/bin/bash
# Creates one database and one login role per service in pg-main.
# Each role owns only its database; PUBLIC cannot connect to any of them.
set -euo pipefail

create_database() {
    local database="$1" role="$2" password="$3"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
        --set=db="$database" --set=usr="$role" --set=pwd="$password" <<'SQL'
CREATE ROLE :"usr" LOGIN PASSWORD :'pwd';
CREATE DATABASE :"db" OWNER :"usr";
REVOKE ALL ON DATABASE :"db" FROM PUBLIC;
SQL
}

create_database identity_db identity "$IDENTITY_DB_PASSWORD"
create_database election_db election "$ELECTION_DB_PASSWORD"
create_database voter_access_db voter_access "$VOTER_ACCESS_DB_PASSWORD"
create_database keycloak_db keycloak "$KEYCLOAK_DB_PASSWORD"
