#!/bin/bash
# Bootstrappt den App-User auf Render OHNE manuelle Eingabe von Credentials -
# liest alles aus .env.render. Nutzt dasselbe zugrundeliegende Skript wie
# lokal (backend/db-init/bootstrap-app-user.sh), verbindet sich aber direkt
# per psql vom eigenen Rechner aus gegen die Render-Postgres-Instanz.
#
# Nutzung:
#   cp .env.render.example .env.render   (einmalig, dann Werte eintragen)
#   ./bootstrap-render.sh
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env.render ]; then
    echo "Fehler: .env.render nicht gefunden. Erst 'cp .env.render.example .env.render' ausfuehren und Werte setzen."
    exit 1
fi

set -a
# shellcheck disable=SC1091
source .env.render
set +a

PGHOST="$PGHOST" PGPORT="${PGPORT:-5432}" PGUSER="$PGUSER" PGPASSWORD="$PGPASSWORD" PGDATABASE="$PGDATABASE" \
APP_DB_USER="$APP_DB_USER" APP_DB_PASSWORD="$APP_DB_PASSWORD" \
    ./backend/db-init/bootstrap-app-user.sh
