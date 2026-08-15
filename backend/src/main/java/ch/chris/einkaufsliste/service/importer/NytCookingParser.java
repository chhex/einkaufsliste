package ch.chris.einkaufsliste.service.importer;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * (Link, Footer) wird ignoriert.
 */
@Component
public class NytCookingParser implements GroceryListParser {

    private static final Pattern LEADING_QUANTITY = Pattern.compile(
            "^\\s*([0-9]*[¼½¾⅓⅔⅛⅜⅝⅞]?[0-9]*(?:\\.[0-9]+)?)\\s+(.*)$");

    private static final Map<Character, BigDecimal> UNICODE_FRACTIONS = Map.of(
            '¼', new BigDecimal("0.25"),
            '½', new BigDecimal("0.5"),
            '¾', new BigDecimal("0.75"),
            '⅓', new BigDecimal("0.3333"),
            '⅔', new BigDecimal("0.6667"),
            '⅛', new BigDecimal("0.125"),
            '⅜', new BigDecimal("0.375"),
            '⅝', new BigDecimal("0.625"),
            '⅞', new BigDecimal("0.875")
    );

    // Bekannte Einheiten-Woerter direkt nach der Menge - alles andere bleibt
    // Teil der Bezeichnung (z.B. "3 garlic cloves" -> Einheit "Stk", da
    // "cloves" hier eher zaehlendes Substantiv als echte Masseinheit ist;
    // das ist eine bewusste Vereinfachung, siehe Klassenkommentar/Vorschau).
    private static final List<String> KNOWN_UNITS = List.of(
            "tablespoons", "tablespoon", "tbsp",
            "teaspoons", "teaspoon", "tsp",
            "cups", "cup",
            "pounds", "pound", "lb", "lbs",
            "ounces", "ounce", "oz",
            "grams", "gram", "g",
            "kilograms", "kilogram", "kg",
            "milliliters", "milliliter", "ml",
            "liters", "liter", "l"
    );

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
            result.add(parseIngredientLine(line));
        }
        return result;
    }

    private ParsedItem parseIngredientLine(String line) {
        Matcher matcher = LEADING_QUANTITY.matcher(line);
        if (!matcher.matches()) {
            // Keine erkennbare Menge am Zeilenanfang - ganze Zeile wird
            // Bezeichnung, Menge/Einheit auf sinnvollen Default.
            return new ParsedItem(line, BigDecimal.ONE, "Stk");
        }

        BigDecimal menge = parseQuantity(matcher.group(1));
        String rest = matcher.group(2);

        for (String unit : KNOWN_UNITS) {
            if (rest.toLowerCase().startsWith(unit + " ") || rest.toLowerCase().equals(unit)) {
                String bezeichnung = rest.substring(unit.length()).strip();
                return new ParsedItem(bezeichnung, menge, unit);
            }
        }

        // Kein bekanntes Einheiten-Wort direkt nach der Menge (z.B.
        // "3 garlic cloves, grated" oder "1 lime, halved") - Default "Stk",
        // kompletter Rest bleibt Teil der Bezeichnung.
        return new ParsedItem(rest, menge, "Stk");
    }

    private BigDecimal parseQuantity(String raw) {
        if (raw.isEmpty()) {
            return BigDecimal.ONE;
        }

        BigDecimal whole = BigDecimal.ZERO;
        BigDecimal fraction = BigDecimal.ZERO;
        StringBuilder wholePart = new StringBuilder();

        for (char c : raw.toCharArray()) {
            if (UNICODE_FRACTIONS.containsKey(c)) {
                fraction = UNICODE_FRACTIONS.get(c);
            } else {
                wholePart.append(c);
            }
        }

        if (!wholePart.isEmpty()) {
            whole = new BigDecimal(wholePart.toString());
        }

        return whole.add(fraction).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
    }

}
