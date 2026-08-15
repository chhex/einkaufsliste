package ch.chris.einkaufsliste.service.importer;

import java.util.List;

/**
 * Ein Parser pro Import-Quelle (Strategy Pattern). Neue Formate = neue
 * Implementierung, ImportService bleibt unveraendert (Spring sammelt alle
 * Beans automatisch ein, siehe ImportService-Konstruktor).
 */
public interface GroceryListParser {

    ImportSource getSource();

    List<ParsedItem> parse(String rawText);

}
