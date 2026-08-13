#!/bin/bash
# Legt den eingeschraenkten Schema-/App-User an (nur DML-Rechte). Wird EINMALIG
# manuell ausgefuehrt, gegen JEDE Postgres-Instanz - egal ob lokal (docker
# compose) oder eine gemanagte DB (z.B. Render), die kein automatisches
# Init-Skript unterstuetzt. Der Admin-User (Schema-Owner) wird bei einer
# gemanagten DB von der Plattform selbst bereitgestellt, lokal ist er der
# POSTGRES_USER aus docker-compose.yml.
#
# Verbindung erfolgt ueber Standard-psql-Umgebungsvariablen - dieselbe
# Aufrufweise, egal wohin:
#
#   PGHOST=localhost PGPORT=5432 PGDATABASE=einkaufsliste \
#   PGUSER=einkaufsliste_admin PGPASSWORD=... \
#   APP_DB_USER=einkaufsliste_app APP_DB_PASSWORD=... \
#   ./bootstrap-app-user.sh
#
# Fuer Render: PGHOST/PGUSER/PGPASSWORD/PGDATABASE aus der von Render
# bereitgestellten Admin-Connection-Info uebernehmen, APP_DB_USER/
# APP_DB_PASSWORD selbst frei waehlen (siehe .env.example).
set -euo pipefail

: "${APP_DB_USER:?APP_DB_USER muss gesetzt sein}"
: "${APP_DB_PASSWORD:?APP_DB_PASSWORD muss gesetzt sein}"

echo "Lege App-User '${APP_DB_USER}' auf ${PGHOST:-localhost}/${PGDATABASE:-postgres} an..."

psql -v ON_ERROR_STOP=1 <<-EOSQL
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${APP_DB_USER}') THEN
            CREATE USER ${APP_DB_USER} WITH PASSWORD '${APP_DB_PASSWORD}';
        END IF;
    END
    \$\$;

    GRANT CONNECT ON DATABASE ${PGDATABASE} TO ${APP_DB_USER};
    GRANT USAGE ON SCHEMA public TO ${APP_DB_USER};

    -- Nur DML, keine DDL-Rechte fuer den App-User
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ${APP_DB_USER};
    GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ${APP_DB_USER};

    -- Gilt auch fuer Tabellen/Sequenzen, die Flyway erst spaeter anlegt
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${APP_DB_USER};
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO ${APP_DB_USER};
EOSQL

echo "Fertig."
