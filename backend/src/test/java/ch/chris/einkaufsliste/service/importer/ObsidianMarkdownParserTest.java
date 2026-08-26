package ch.chris.einkaufsliste.service.importer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reiner Unit-Test: kein Spring-Context, kein Docker.
 */
class ObsidianMarkdownParserTest {

    private final ObsidianMarkdownParser parser = new ObsidianMarkdownParser();

    private static final String BEISPIEL = """
            ---
            tags: einkaufen
            ---
            # Wocheneinkauf

            ## Gemüse
            - [ ] Tomaten
            - [ ] 2 Zwiebeln

            ## Milchprodukte
            - [x] Milch
            - [ ] 500g Butter

            Ein Kommentar, der ignoriert werden sollte.
            """;

    @Test
    void getSourceLiefertObsidianMarkdown() {
        assertThat(parser.getSource()).isEqualTo(ImportSource.OBSIDIAN_MARKDOWN);
    }

    @Test
    void erkenntErwarteteAnzahlItems() {
        List<ParsedItem> items = parser.parse(BEISPIEL);

        assertThat(items).hasSize(4);
    }

    @Test
    void uebernimmtUeberschriftenAlsKategorie() {
        List<ParsedItem> items = parser.parse(BEISPIEL);

        ParsedItem tomaten = items.stream()
                .filter(i -> i.bezeichnung().equals("Tomaten"))
                .findFirst().orElseThrow();
        assertThat(tomaten.kategorie()).isEqualTo("Gemüse");

        ParsedItem milch = items.stream()
                .filter(i -> i.bezeichnung().equals("Milch"))
                .findFirst().orElseThrow();
        assertThat(milch.kategorie()).isEqualTo("Milchprodukte");
    }

    @Test
    void ignoriertAbhakStatusImportiertImmerAlsOffen() {
        // "- [x] Milch" ist im Quelltext bereits abgehakt - ParsedItem hat
        // aber gar kein abgehakt-Feld, weil importierte Items IMMER offen
        // starten (bewusste Entscheidung, siehe Klassenkommentar).
        List<ParsedItem> items = parser.parse(BEISPIEL);

        assertThat(items).extracting(ParsedItem::bezeichnung).contains("Milch");
    }

    @Test
    void erkenntMengeUndEinheitInnerhalbEinerZeile() {
        List<ParsedItem> items = parser.parse(BEISPIEL);

        ParsedItem zwiebeln = items.stream()
                .filter(i -> i.bezeichnung().equals("Zwiebeln"))
                .findFirst().orElseThrow();
        assertThat(zwiebeln.menge()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(zwiebeln.einheit()).isEqualTo("Stk"); // "Zwiebeln" ist keine bekannte Einheit

        ParsedItem butter = items.stream()
                .filter(i -> i.bezeichnung().equals("Butter"))
                .findFirst().orElseThrow();
        assertThat(butter.menge()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(butter.einheit()).isEqualTo("g");
    }

    @Test
    void ignoriertFrontmatterTitelUndProsa() {
        List<ParsedItem> items = parser.parse(BEISPIEL);

        assertThat(items).noneMatch(i -> i.bezeichnung().contains("tags"));
        assertThat(items).noneMatch(i -> i.bezeichnung().contains("Wocheneinkauf"));
        assertThat(items).noneMatch(i -> i.bezeichnung().contains("Kommentar"));
    }

    @Test
    void listeOhneCheckboxenLiefertLeereListe() {
        List<ParsedItem> items = parser.parse("# Nur ein Titel\n\nEin bisschen Prosa ohne Checkboxen.");

        assertThat(items).isEmpty();
    }

    @Test
    void unterstuetztSowohlBindestrichAlsAuchSternchenAlsListenzeichen() {
        List<ParsedItem> items = parser.parse("* [ ] Kaffee\n- [ ] Tee");

        assertThat(items).extracting(ParsedItem::bezeichnung).containsExactlyInAnyOrder("Kaffee", "Tee");
    }

}
