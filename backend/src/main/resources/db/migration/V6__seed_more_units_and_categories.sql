-- Ergaenzung der Vorschlagslisten basierend auf echtem Feedback nach dem
-- ersten Coop-Einkauf mit der App. V3 wurde bereits produktiv angewendet -
-- daher eine neue Migration statt V3 nachtraeglich zu aendern.

INSERT INTO unit (name, abbreviation) VALUES
    ('Flasche', 'Fl'),
    ('kleiner Becher', 'kl. Bch'),
    ('grosser Becher', 'gr. Bch');

INSERT INTO category (name) VALUES
    ('Gewürze');
