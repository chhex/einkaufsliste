-- Anforderung (Feedback aus echtem Erst-Einsatz): nur der Artikelname soll
-- beim Hinzufuegen Pflicht sein, Menge/Einheit koennen spaeter nachgetragen
-- werden - schnelles Erfassen im Laden ("Milch" hinschreiben, Details
-- spaeter).

ALTER TABLE item ALTER COLUMN menge DROP NOT NULL;
ALTER TABLE item ALTER COLUMN einheit DROP NOT NULL;
