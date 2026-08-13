-- Schritt 3a: Domain-Schema fuer die Einkaufslisten-App.
-- Ersetzt die provisorische Baseline (V1) durch die echten Kern-Tabellen.

-- Nutzer (Google OAuth2 Login)
CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    google_id     VARCHAR(255) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Globale Kategorien-Vorschlagsliste (z.B. "Gemuese", "Milchprodukte").
-- Analog zu "unit": dient nur als Vorschlagsliste/Autocomplete fuer die UI,
-- KEIN FK von item aus. Die Kategorie am Item ist Freitext (siehe unten),
-- weil "Kategorie" subjektiv/situativ ist und individuell ueberschrieben
-- werden koennen soll.
CREATE TABLE category (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL UNIQUE
);

-- Erweiterbare Einheiten-Tabelle: dient nur als Vorschlagsliste/Autocomplete
-- fuer die UI, wird per Seed-Migration (V3) vorbefuellt. KEIN FK von item aus -
-- die Einheit am Item selbst ist Freitext (siehe unten), damit Imports mit
-- beliebigen/fremden Masssystemen (z.B. "cups", "tbsp") oder spontane
-- Nutzereingaben ("Flasche", "Prise") nicht an ein starres Vokabular gebunden sind.
CREATE TABLE unit (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(50)  NOT NULL UNIQUE,
    abbreviation  VARCHAR(10)  NOT NULL UNIQUE
);

-- Einkaufsliste.
-- "sortierung" ist die Default-/Owner-Sortierpraeferenz (nach welchem
-- Freitext-Feld die Items angezeigt werden). Einzelne Mitglieder koennen das
-- ueber list_member.sortierung individuell ueberschreiben.
CREATE TABLE list (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    owner_id      BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'AKTIV'
                  CHECK (status IN ('AKTIV', 'ARCHIVIERT')),
    sortierung    VARCHAR(20)  NOT NULL DEFAULT 'kategorie'
                  CHECK (sortierung IN ('kategorie', 'bezeichnung', 'einheit')),
    archived_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_list_owner ON list(owner_id);
CREATE INDEX idx_list_status ON list(status);

-- Mitgliedschaft: welche User duerfen auf welche Liste zugreifen (neben dem Owner).
-- "sortierung" ist NULLABLE: NULL bedeutet "kein eigenes Override, nutze
-- list.sortierung"; gesetzt bedeutet individuelle Praeferenz dieses Members.
CREATE TABLE list_member (
    list_id       BIGINT      NOT NULL REFERENCES list(id) ON DELETE CASCADE,
    user_id       BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    sortierung    VARCHAR(20) CHECK (sortierung IN ('kategorie', 'bezeichnung', 'einheit')),
    added_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (list_id, user_id)
);

-- Einzelnes Item innerhalb einer Liste.
-- "einheit" und "kategorie" sind bewusst Freitext (kein FK auf unit/category)
-- - siehe Kommentare bei den jeweiligen Vorschlagslisten-Tabellen oben.
CREATE TABLE item (
    id            BIGSERIAL PRIMARY KEY,
    list_id       BIGINT        NOT NULL REFERENCES list(id) ON DELETE CASCADE,
    bezeichnung   VARCHAR(255)  NOT NULL,
    menge         NUMERIC(10,2) NOT NULL,
    einheit       VARCHAR(50)   NOT NULL,
    kategorie     VARCHAR(100),
    abgehakt      BOOLEAN       NOT NULL DEFAULT false,
    abgehakt_am   TIMESTAMPTZ,
    reihenfolge   INT           NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_item_list ON item(list_id);
CREATE INDEX idx_item_list_abgehakt ON item(list_id, abgehakt);
