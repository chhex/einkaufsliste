package ch.chris.einkaufsliste.service.importer;

import java.math.BigDecimal;

/**
 * Ein aus Rohtext geparstes Item, VOR der Uebernahme in eine Liste - dient
 * der Vorschau (Anforderung 6: "Vorschau vor Uebernahme"), damit der Nutzer
 * Fehlinterpretationen korrigieren kann, bevor daraus ein echtes Item wird.
 * Kategorie ist absichtlich nicht Teil davon - bleibt beim Import immer
 * "unkategorisiert" (siehe Anforderungsdokument).
 */
public record ParsedItem(String bezeichnung, BigDecimal menge, String einheit) {
}
