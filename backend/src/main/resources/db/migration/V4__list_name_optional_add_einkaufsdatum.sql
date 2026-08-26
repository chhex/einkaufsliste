-- Anforderung: Liste anlegen soll reibungslos gehen (kein Pflicht-Name),
-- stattdessen ein Einkaufsdatum, das per Default das Erfassungsdatum ist,
-- aber jederzeit geaendert werden kann.

ALTER TABLE list ALTER COLUMN name DROP NOT NULL;

ALTER TABLE list ADD COLUMN einkaufsdatum DATE NOT NULL DEFAULT CURRENT_DATE;

-- Der Default gilt nur fuer kuenftige Inserts ohne expliziten Wert (Safety-
-- Net); die Anwendung selbst setzt einkaufsdatum immer explizit (siehe
-- ShoppingList-Entity).
