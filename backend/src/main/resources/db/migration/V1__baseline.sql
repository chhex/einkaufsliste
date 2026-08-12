-- Baseline-Migration fuer Schritt 2 (Docker-Datenbank testen).
-- Verifiziert, dass Flyway (als Admin-User) Migrationen ausfuehren kann und
-- der App-User (DML-only) danach lesen/schreiben darf.
-- Wird in Schritt 3 (Daten-Access-Layer) durch die echten Domain-Tabellen
-- (list, item, category, unit, ...) abgeloest/ergaenzt.
CREATE TABLE schema_smoke_test (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
