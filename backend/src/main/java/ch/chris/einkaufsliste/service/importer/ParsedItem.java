package ch.chris.einkaufsliste.service.importer;

import java.math.BigDecimal;

/**
 * Ein aus Rohtext geparstes Item, VOR der Uebernahme in eine Liste - dient
 * der Vorschau (Anforderung 6: "Vorschau vor Uebernahme"), damit der Nutzer
 * Fehlinterpretationen korrigieren kann, bevor daraus ein echtes Item wird.
 * kategorie ist nullable ("unkategorisiert", siehe Anforderungsdokument);
 * ObsidianMarkdownParser leitet sie aus Ueberschriften/**Fett**-Zeilen ab,
 * DelimitedTextParser aus dem optionalen Kategorie-Feld.
 */
public record ParsedItem(String bezeichnung, BigDecimal menge, String einheit, String kategorie) {
}
