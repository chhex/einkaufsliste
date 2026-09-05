#!/bin/bash
# Bootstrappt den lokalen App-User OHNE manuelle Eingabe von Credentials -
# liest alles automatisch aus .env. Voraussetzung: "docker compose up -d db"
# laeuft bereits.
#
# Nutzung:
#   ./bootstrap-local.sh
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
    echo "Fehler: .env nicht gefunden. Erst 'cp .env.example .env' ausfuehren und Werte setzen."
    exit 1
fi

# Nur die Zeilen aus .env laden, die wir brauchen (ignoriert Kommentare/Leerzeilen)
set -a
# shellcheck disable=SC1091
source .env
set +a

docker compose -f docker-compose.prod.yml exec db sh -c \
    "PGHOST=localhost PGUSER=\$POSTGRES_USER PGPASSWORD=\$POSTGRES_PASSWORD PGDATABASE=\$POSTGRES_DB \
     APP_DB_USER='${DB_APP_USER}' APP_DB_PASSWORD='${DB_APP_PASSWORD}' \
     /scripts/bootstrap-app-user.sh"

