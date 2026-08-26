package ch.chris.einkaufsliste.service.importer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Erkennt Menge + Einheit am Anfang einer Freitext-Zeile (z.B.
 * "1½ tablespoons ginger" oder "2 Zwiebeln") und liefert den Rest als
 * Bezeichnung. Gemeinsam genutzt von allen Import-Parsern (Strategy Pattern,
 * siehe GroceryListParser) - vermeidet Duplikation der Mengen/Einheit-Logik.
 * <p>
 * Bewusst pragmatisch/heuristisch (siehe Klassenkommentare der Parser): bei
 * Unklarheit lieber ganze Zeile als Bezeichnung mit Default-Menge/-Einheit,
 * als eine falsche Interpretation zu erzwingen - die Vorschau vor Uebernahme
 * (Anforderung 6) faengt Fehlinterpretationen ohnehin ab.
 */
final class IngredientTextParser {

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
    // "cloves" hier eher zaehlendes Substantiv als echte Masseinheit ist).
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

    private IngredientTextParser() {
        // reine Utility-Klasse
    }

    static ParsedItem parse(String line, String kategorie) {
        Matcher matcher = LEADING_QUANTITY.matcher(line);
        if (!matcher.matches()) {
            // Keine erkennbare Menge am Zeilenanfang - ganze Zeile wird
            // Bezeichnung, Menge/Einheit auf sinnvollen Default.
            return new ParsedItem(line, BigDecimal.ONE, "Stk", kategorie);
        }

        BigDecimal menge = parseQuantity(matcher.group(1));
        String rest = matcher.group(2);

        for (String unit : KNOWN_UNITS) {
            if (rest.toLowerCase().startsWith(unit + " ") || rest.toLowerCase().equals(unit)) {
                String bezeichnung = rest.substring(unit.length()).strip();
                return new ParsedItem(bezeichnung, menge, unit, kategorie);
            }
        }

        // Kein bekanntes Einheiten-Wort direkt nach der Menge (z.B.
        // "3 garlic cloves, grated" oder "2 Zwiebeln") - Default "Stk",
        // kompletter Rest bleibt Teil der Bezeichnung.
        return new ParsedItem(rest, menge, "Stk", kategorie);
    }

    private static BigDecimal parseQuantity(String raw) {
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
