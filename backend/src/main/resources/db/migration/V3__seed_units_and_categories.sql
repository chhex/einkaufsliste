-- Vordefinierte, sinnvolle Einheiten (Tabelle bleibt erweiterbar,
-- Nutzer koennen spaeter eigene ergaenzen)
INSERT INTO unit (name, abbreviation) VALUES
    ('Stück',    'Stk'),
    ('Kilogramm','kg'),
    ('Gramm',    'g'),
    ('Liter',    'l'),
    ('Milliliter','ml'),
    ('Becher',   'Bch'),
    ('Bund',     'Bd'),
    ('Packung',  'Pkg'),
    ('Dose',     'Dse');

-- Basis-Kategorien zum Start (reine Vorschlagsliste/Autocomplete, keine
-- feste Sortierung mehr noetig - die Anzeige-Sortierung kommt jetzt ueber
-- list.sortierung / list_member.sortierung, siehe V2)
INSERT INTO category (name) VALUES
    ('Gemüse'),
    ('Obst'),
    ('Milchprodukte'),
    ('Fleisch & Fisch'),
    ('Tiefkühl'),
    ('Getränke'),
    ('Backwaren'),
    ('Vorräte & Konserven'),
    ('Haushalt'),
    ('Sonstiges');
