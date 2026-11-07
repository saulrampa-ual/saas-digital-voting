#!/bin/bash
# Creates the ballot box database in its own PostgreSQL instance (pg-ballot).
# No other service has a role here (RNF-11: participation and ballot box never share a database).
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
    --set=pwd="$BALLOT_BOX_DB_PASSWORD" <<'SQL'
CREATE ROLE ballot_box LOGIN PASSWORD :'pwd';
CREATE DATABASE ballot_box_db OWNER ballot_box;
REVOKE ALL ON DATABASE ballot_box_db FROM PUBLIC;
SQL
