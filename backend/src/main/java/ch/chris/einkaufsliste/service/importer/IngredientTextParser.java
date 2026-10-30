package ch.chris.einkaufsliste.service.importer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Erkennt die Anzahl am Anfang einer Freitext-Zeile (z.B. "1½ ...", "3 to 4 ..."
 * oder "4,4kg ...") und liefert den Rest als Bezeichnung.
 * <p>
 * Einheiten werden NICHT geraten: Nur wenn Buchstaben DIREKT (ohne Leerzeichen)
 * an der Zahl kleben ("4,4kg", "2Stück", "1Bund"), gilt dieses Wort woertlich
 * als Einheit (kein Alias-Mapping, "Stück" bleibt "Stück"). Mit Leerzeichen
 * ("2 Zwiebeln", "1½ tablespoons ...") bleibt der komplette Rest Bezeichnung
 * und die Einheit leer.
 * <p>
 * Dezimalkomma und Dezimalpunkt werden akzeptiert ("4,4" = "4.4").
 * <p>
 * Ist die Anzahl ein Bereich ("3 to 4", "3-4"), wird der HOEHERE Wert
 * genommen (Anforderung: lieber zu viel einkaufen als zu wenig).
 * <p>
 * Gemeinsam genutzt von allen Import-Parsern (Strategy Pattern, siehe
 * GroceryListParser) - vermeidet Duplikation der Mengen-Logik.
 */
final class IngredientTextParser {

    private static final String NUM = "[0-9]*[¼½¾⅓⅔⅛⅜⅝⅞]?[0-9]*(?:[.,][0-9]+)?";

    // Gruppen: 1 = Zahl, 2 = obere Grenze eines Bereichs (optional, muss mit
    // Ziffer/Bruch beginnen), 3 = Leerraum nach der Zahl (leer = Einheit klebt
    // direkt an der Zahl), 4 = Rest der Zeile.
    private static final Pattern LEADING_QUANTITY = Pattern.compile(
            "^\\s*(" + NUM + ")"
                    + "(?:\\s*(?:to|bis|-)\\s*((?=[0-9¼½¾⅓⅔⅛⅜⅝⅞])" + NUM + "))?"
                    + "(\\s*)(.*)$");

    private static final Pattern GLUED_UNIT = Pattern.compile("^(\\p{L}+)(.*)$");

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

        BigDecimal menge = mengeOf(matcher);

        String rest = matcher.group(4);
        boolean zahlVorhanden = !matcher.group(1).isEmpty();
        boolean klebtAnZahl = matcher.group(3).isEmpty();

        if (zahlVorhanden && klebtAnZahl) {
            Matcher unit = GLUED_UNIT.matcher(rest);
            if (unit.matches() && !unit.group(2).strip().isEmpty()) {
                // "4,4kg Rindsschulter" -> Einheit "kg" (woertlich), Name "Rindsschulter"
                return new ParsedItem(unit.group(2).strip(), menge, unit.group(1), kategorie);
            }
        }

        return new ParsedItem(rest.strip(), menge, null, kategorie);
    }

    /** Menge (bei Bereichen der hoehere Wert) aus den Gruppen 1/2 des Treffers. */
    private static BigDecimal mengeOf(Matcher matcher) {
        BigDecimal menge = parseQuantity(matcher.group(1));
        String zweiteZahl = matcher.group(2);
        if (zweiteZahl != null && !zweiteZahl.isEmpty()) {
            // Bereichsangabe (z.B. "3 to 4") - der HOEHERE Wert gewinnt.
            BigDecimal obereGrenze = parseQuantity(zweiteZahl);
            if (obereGrenze.compareTo(menge) > 0) {
                menge = obereGrenze;
            }
        }
        return menge;
    }

    /** Reine Mengenangabe: Zahl, optional mit Einheit. */
    record Quantity(BigDecimal menge, String einheit) {
    }

    /**
     * Prueft, ob der Text AUSSCHLIESSLICH aus Menge (+ optionaler Einheit)
     * besteht, z.B. "1kg", "2", "500 g". Liefert sonst null.
     *
     * @param erlaubeLeerzeichenVorEinheit false: Einheit muss direkt an der Zahl
     *                                     kleben ("1kg"), sonst waere "2 Zwiebeln"
     *                                     nicht von "2 Stueck" unterscheidbar.
     */
    static Quantity parseQuantityOnly(String text, boolean erlaubeLeerzeichenVorEinheit) {
        Matcher matcher = LEADING_QUANTITY.matcher(text.strip());
        if (!matcher.matches() || matcher.group(1).isEmpty()) {
            return null;
        }
        BigDecimal menge = mengeOf(matcher);
        String rest = matcher.group(4).strip();
        if (rest.isEmpty()) {
            return new Quantity(menge, null);
        }
        if (!rest.matches("\\p{L}+")) {
            return null;
        }
        boolean klebt = matcher.group(3).isEmpty();
        return (klebt || erlaubeLeerzeichenVorEinheit) ? new Quantity(menge, rest) : null;
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
            whole = new BigDecimal(wholePart.toString().replace(',', '.'));
        }

        return whole.add(fraction).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
    }

}
