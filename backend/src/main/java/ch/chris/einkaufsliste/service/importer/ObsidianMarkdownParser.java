package ch.chris.einkaufsliste.service.importer;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parst Obsidian-Markdown-Checklisten.
 * <p>
 * Format-Beispiel:
 * <pre>
 * ---
 * tags: einkaufen
 * ---
 * # Wocheneinkauf
 *
 * ## Gemüse
 * - [ ] Tomaten
 * - [ ] 2 Zwiebeln
 *
 * ## Milchprodukte
 * - [x] Milch
 * - [ ] 500g Butter
 * </pre>
 * Anders als bei NytCookingParser (Start-/End-Marker-Suche) ist hier das
 * Checkbox-Muster selbst der eindeutige Marker: jede Zeile, die NICHT
 * "- [ ]"/"- [x]" entspricht (Frontmatter, Titel, Prosa, Leerzeilen), wird
 * einfach ignoriert - kein Suchen nach Anfang/Ende noetig.
 * <p>
 * Markdown-Ueberschriften (#, ##, ...) werden als Kategorie fuer alle
 * nachfolgenden Items uebernommen, bis zur naechsten Ueberschrift.
 * <p>
 * Der Abhak-Status ("[x]") wird bewusst IGNORIERT - importierte Items
 * starten immer offen (Entscheidung: Haken in einer wiederverwendeten
 * Obsidian-Vorlage spiegeln meist den letzten Durchgang, nicht den
 * aktuellen Stand).
 */
@Component
public class ObsidianMarkdownParser implements GroceryListParser {

    private static final Pattern HEADER = Pattern.compile("^#{1,6}\\s+(.*)$");
    private static final Pattern CHECKBOX = Pattern.compile("^\\s*[-*]\\s*\\[[ xX]]\\s*(.*)$");

    @Override
    public ImportSource getSource() {
        return ImportSource.OBSIDIAN_MARKDOWN;
    }

    @Override
    public List<ParsedItem> parse(String rawText) {
        List<ParsedItem> result = new ArrayList<>();
        String currentKategorie = null;

        for (String rawLine : rawText.lines().toList()) {
            String line = rawLine.strip();
            if (line.isBlank()) {
                continue;
            }

            Matcher headerMatcher = HEADER.matcher(line);
            if (headerMatcher.matches()) {
                currentKategorie = headerMatcher.group(1).strip();
                continue;
            }

            Matcher checkboxMatcher = CHECKBOX.matcher(line);
            if (checkboxMatcher.matches()) {
                String itemText = checkboxMatcher.group(1).strip();
                if (!itemText.isEmpty()) {
                    result.add(IngredientTextParser.parse(itemText, currentKategorie));
                }
            }
            // alle anderen Zeilen (Frontmatter, Titel ohne #, Prosa) werden
            // stillschweigend ignoriert
        }

        return result;
    }

}
