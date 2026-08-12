#!/bin/bash
# Legt den eingeschraenkten Schema-/App-User an, der zur Laufzeit von Spring
# Boot genutzt wird. Der Admin-User (POSTGRES_USER) besitzt das Schema und
# fuehrt die Flyway-Migrationen aus. Wird beim ersten Start des Postgres-
# Containers automatisch ausgefuehrt (docker-entrypoint-initdb.d).
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER ${APP_DB_USER} WITH PASSWORD '${APP_DB_PASSWORD}';

    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${APP_DB_USER};
    GRANT USAGE ON SCHEMA public TO ${APP_DB_USER};

    -- Nur DML, keine DDL-Rechte fuer den App-User
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ${APP_DB_USER};
    GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ${APP_DB_USER};

    -- Gilt auch fuer Tabellen/Sequenzen, die Flyway erst spaeter (Schritt 3) anlegt
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${APP_DB_USER};
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO ${APP_DB_USER};
EOSQL
