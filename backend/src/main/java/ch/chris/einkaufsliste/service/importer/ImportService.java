package ch.chris.einkaufsliste.service.importer;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Waehlt den passenden Parser explizit ueber ImportSource aus (keine
 * Auto-Erkennung). Neue Import-Formate = neue GroceryListParser-Implementierung
 * (z.B. @Component class MigrosAppParser implements GroceryListParser),
 * Spring injiziert automatisch alle Parser-Beans - diese Klasse bleibt
 * unveraendert (Open/Closed-Prinzip).
 */
@Service
public class ImportService {

    private final Map<ImportSource, GroceryListParser> parsers;

    public ImportService(List<GroceryListParser> parserBeans) {
        this.parsers = parserBeans.stream()
                .collect(Collectors.toMap(GroceryListParser::getSource, Function.identity()));
    }

    /**
     * Parst Rohtext zu einer Vorschau-Liste (ParsedItem), noch NICHT in
     * echte Item-Entities uebernommen - der Nutzer bestaetigt/korrigiert
     * zuerst (Anforderung 6).
     */
    public List<ParsedItem> parse(ImportSource source, String rawText) {
        GroceryListParser parser = parsers.get(source);
        if (parser == null) {
            throw new IllegalArgumentException("Kein Parser fuer Import-Quelle: " + source);
        }
        return parser.parse(rawText);
    }

}
