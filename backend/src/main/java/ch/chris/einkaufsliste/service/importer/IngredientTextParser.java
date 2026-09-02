package ch.chris.einkaufsliste.service.importer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Erkennt nur noch die Anzahl am Anfang einer Freitext-Zeile (z.B. "1½ ..."
 * oder "3 to 4 ...") und liefert den KOMPLETTEN Rest als Bezeichnung -
 * KEIN Einheit-Raten mehr (fruehere Version versuchte, ein bekanntes
 * Einheiten-Wort direkt nach der Menge zu erkennen und abzutrennen; das
 * lieferte zu oft falsche/unpassende Ergebnisse). Einheit bleibt bewusst
 * leer und wird vom Nutzer in der Vorschau oder spaeter direkt am Item
 * nachgetragen (seit Kurzem ohnehin optional, siehe Item-Entity).
 * <p>
 * Ist die Anzahl ein Bereich ("3 to 4", "3-4"), wird der HOEHERE Wert
 * genommen (Anforderung: lieber zu viel einkaufen als zu wenig).
 * <p>
 * Gemeinsam genutzt von allen Import-Parsern (Strategy Pattern, siehe
 * GroceryListParser) - vermeidet Duplikation der Mengen-Logik.
 */
final class IngredientTextParser {

    // Eine Zahl (inkl. optionalem Unicode-Bruch), optional gefolgt von
    // "bis"/"to"/"-" und einer zweiten Zahl (Bereich) - danach der Rest der
    // Zeile als Bezeichnung. Trennzeichen zwischen Zahl und Rest ist absichtlich
    // OPTIONAL (\s*, nicht \s+): Formate wie "500g Butter" (Menge klebt direkt
    // an der Einheit, kein Leerzeichen) sollen die Menge trotzdem erkennen -
    // "g" landet dann einfach als Teil der Bezeichnung ("g Butter"), da wir
    // keine Einheit mehr herausparsen.
    private static final Pattern LEADING_QUANTITY = Pattern.compile(
            "^\\s*([0-9]*[¼½¾⅓⅔⅛⅜⅝⅞]?[0-9]*(?:\\.[0-9]+)?)"
                    + "(?:\\s*(?:to|bis|-)\\s*([0-9]*[¼½¾⅓⅔⅛⅜⅝⅞]?[0-9]*(?:\\.[0-9]+)?))?"
                    + "\\s*(.*)$");

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

    private IngredientTextParser() {
        // reine Utility-Klasse
    }

    static ParsedItem parse(String line, String kategorie) {
        Matcher matcher = LEADING_QUANTITY.matcher(line);
        if (!matcher.matches()) {
            // Keine erkennbare Menge am Zeilenanfang - ganze Zeile wird
            // Bezeichnung, Menge auf sinnvollen Default, Einheit leer.
            return new ParsedItem(line, BigDecimal.ONE, null, kategorie);
        }

        BigDecimal menge = parseQuantity(matcher.group(1));
        String zweiteZahl = matcher.group(2);
        if (zweiteZahl != null && !zweiteZahl.isEmpty()) {
            // Bereichsangabe (z.B. "3 to 4") - der HOEHERE Wert gewinnt.
            BigDecimal obereGrenze = parseQuantity(zweiteZahl);
            if (obereGrenze.compareTo(menge) > 0) {
                menge = obereGrenze;
            }
        }

        String bezeichnung = matcher.group(3).strip();
        return new ParsedItem(bezeichnung, menge, null, kategorie);
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
