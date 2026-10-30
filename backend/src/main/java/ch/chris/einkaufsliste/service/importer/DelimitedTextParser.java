package ch.chris.einkaufsliste.service.importer;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parst einfach formatierten Text, der auch von Hand eingegeben werden kann.
 * <p>
 * Ein Eintrag endet bei {@code ;} oder Zeilenumbruch. Innerhalb eines
 * Eintrags trennen {@code /}, {@code ?} und {@code " - "} (Bindestrich nur
 * mit Leerzeichen auf beiden Seiten) die Felder; die Trenner duerfen in einem
 * Text gemischt vorkommen. Bewusst KEIN Auto-Detect und KEIN Komma/Punkt
 * (Dezimalzahlen, "Aepfel, rot"), kein {@code |} (auf der Schweizer
 * iOS-Tastatur muehsam). Ein {@code /} zwischen zwei Ziffern ("1/2") ist
 * kein Trenner.
 * <p>
 * Feldreihenfolge: {@code [Menge+Einheit] Bezeichnung [Kategorie]}.
 * <ul>
 *   <li>1 Feld: Menge+Einheit am Anfang (falls vorhanden) abtrennen, Rest = Bezeichnung</li>
 *   <li>2 Felder: ist das erste Feld eine reine Menge (Zahl, Einheit direkt
 *       angeklebt, z.B. "1kg") -> Menge + Bezeichnung; sonst Bezeichnung + Kategorie</li>
 *   <li>3 Felder: Menge+Einheit, Bezeichnung, Kategorie</li>
 * </ul>
 * Einheiten werden woertlich uebernommen (kein Mapping).
 */
@Component
public class DelimitedTextParser implements GroceryListParser {

    private static final Pattern ENTRY_SEPARATOR = Pattern.compile("[;\\r\\n]+");
    private static final Pattern FIELD_SEPARATOR = Pattern.compile(
            "\\s*(?:(?<!\\d)/|/(?!\\d)|\\?)\\s*|\\s+-\\s+");

    @Override
    public ImportSource getSource() {
        return ImportSource.DELIMITED_TEXT;
    }

    @Override
    public List<ParsedItem> parse(String rawText) {
        List<ParsedItem> result = new ArrayList<>();
        for (String entry : ENTRY_SEPARATOR.split(rawText)) {
            ParsedItem item = parseEntry(entry.strip());
            if (item != null && !item.bezeichnung().isBlank()) {
                result.add(item);
            }
        }
        return result;
    }

    private ParsedItem parseEntry(String entry) {
        if (entry.isEmpty()) {
            return null;
        }
        List<String> fields = Arrays.stream(FIELD_SEPARATOR.split(entry))
                .map(String::strip)
                .filter(f -> !f.isEmpty())
                .toList();
        if (fields.isEmpty()) {
            return null;
        }

        String first = fields.get(0);
        switch (fields.size()) {
            case 1:
                return IngredientTextParser.parse(first, null);
            case 2: {
                IngredientTextParser.Quantity q = IngredientTextParser.parseQuantityOnly(first, false);
                return q != null
                        ? new ParsedItem(fields.get(1), q.menge(), q.einheit(), null)
                        : IngredientTextParser.parse(first, fields.get(1));
            }
            default: {
                IngredientTextParser.Quantity q = IngredientTextParser.parseQuantityOnly(first, true);
                return q != null
                        ? new ParsedItem(fields.get(1), q.menge(), q.einheit(), fields.get(2))
                        : IngredientTextParser.parse(first, fields.get(1));
            }
        }
    }

}
