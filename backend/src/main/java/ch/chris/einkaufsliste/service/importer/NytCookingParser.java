package ch.chris.einkaufsliste.service.importer;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Parst den Copy-Paste-Export von NYT Cooking ("Your Grocery List").
 * <p>
 * Format-Beispiel:
 * <pre>
 * Your Grocery List
 * 1 Recipe
 *
 * Skillet Meatballs With Peaches, Basil and Lime
 * 3 to 4 servings
 * -
 * 1½ tablespoons finely grated or minced fresh ginger
 * 3 garlic cloves, grated or minced
 * ...
 *
 * ----------
 *
 * View Recipes on NYT Cooking
 * -
 * Skillet Meatballs With Peaches, Basil and Lime:
 * https://cooking.nytimes.com/recipes/...
 * </pre>
 * Erkennungslogik: Zutaten stehen zwischen der ERSTEN Zeile, die nur aus
 * einem einzelnen "-" besteht, und der Zeile, die nur aus mehreren "-"
 * besteht ("----------"). Alles davor (Rezeptname, Portionen) und danach
 * (Link, Footer) wird ignoriert. NYT liefert keine Kategorien - kategorie
 * bleibt hier immer null ("unkategorisiert").
 */
@Component
public class NytCookingParser implements GroceryListParser {

    @Override
    public ImportSource getSource() {
        return ImportSource.NYT_COOKING;
    }

    @Override
    public List<ParsedItem> parse(String rawText) {
        List<String> lines = rawText.lines().map(String::strip).toList();

        int startIndex = -1;
        int endIndex = lines.size();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (startIndex == -1 && line.equals("-")) {
                startIndex = i + 1;
                continue;
            }
            if (startIndex != -1 && line.matches("-{3,}")) {
                endIndex = i;
                break;
            }
        }

        if (startIndex == -1) {
            // Kein erkennbarer Marker gefunden - Format weicht ab, lieber
            // nichts parsen als Muell erzeugen. Vorschau zeigt dann "0 Items"
            // und der Nutzer merkt sofort, dass der Import nicht griff.
            return List.of();
        }

        List<ParsedItem> result = new ArrayList<>();
        for (String line : lines.subList(startIndex, endIndex)) {
            if (line.isBlank()) {
                continue;
            }
            result.add(IngredientTextParser.parse(line, null));
        }
        return result;
    }

}
