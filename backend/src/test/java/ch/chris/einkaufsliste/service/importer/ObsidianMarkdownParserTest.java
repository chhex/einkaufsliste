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
    void erkenntMengeAuchOhneLeerzeichenVorDerEinheit() {
        List<ParsedItem> items = parser.parse(BEISPIEL);

        ParsedItem zwiebeln = items.stream()
                .filter(i -> i.bezeichnung().equals("Zwiebeln"))
                .findFirst().orElseThrow();
        assertThat(zwiebeln.menge()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(zwiebeln.einheit()).isNull(); // mit Leerzeichen: keine Einheit

        // "500g Butter" - Einheit klebt direkt an der Zahl -> woertlich uebernommen.
        ParsedItem butter = items.stream()
                .filter(i -> i.bezeichnung().equals("Butter"))
                .findFirst().orElseThrow();
        assertThat(butter.menge()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(butter.einheit()).isEqualTo("g");
        assertThat(butter.bezeichnung()).isEqualTo("Butter");
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

    private static final String FETT_KATEGORIEN = """


            **Fleisch**
            - [ ] 4,4kg Rindsschulter
            - [ ] 50g Rindertalg

            **Gemüse**
            - [ ] 2Stück Zwiebeln
            - [ ] 1Stück Peperoni rot
            - [ ] 1Stück Peperoni grün
            - [ ] 1Knolle Knoblauch

            **Kräuter**
            - [ ] 1Bund Koriander

            **Früchte**
            - [ ] 1Stück Orange
            - [ ] 2Stück Limetten

            **Konserven**
            - [ ] 800g Gehackte Tomaten
            - [ ] 1Tube Tomatenmark
            - [ ] 80g Grüne Oliven

            **Gewürze**
            - [ ] 1Pack Oregano getrocknet
            - [ ] 1Pack Kreuzkümmel gemahlen
            - [ ] 1Pack Knoblauchpulver
            - [ ] 1Pack Paprika edelsüss
            - [ ] 1Pack Schwarzer Pfeffer grob
            - [ ] 1Pack Lorbeerblätter

            **BBQ**
            - [ ] 1Pack Räucherholz Eiche

            **Haushalt**
            - [ ] 4Stück Vakuumbeutel gross
            ]]
            """;

    private ParsedItem finde(List<ParsedItem> items, String bezeichnung) {
        return items.stream()
                .filter(i -> i.bezeichnung().equals(bezeichnung))
                .findFirst().orElseThrow();
    }

    @Test
    void fettgedruckteZeilenSindKategorienUndKlebendeEinheitenWerdenErkannt() {
        List<ParsedItem> items = parser.parse(FETT_KATEGORIEN);

        // 2 + 4 + 1 + 2 + 3 + 6 + 1 + 1; die abschliessende "]]" wird ignoriert
        assertThat(items).hasSize(20);
        assertThat(items).extracting(ParsedItem::kategorie).doesNotContainNull();
        assertThat(items).extracting(ParsedItem::kategorie).containsOnly(
                "Fleisch", "Gemüse", "Kräuter", "Früchte", "Konserven", "Gewürze", "BBQ", "Haushalt");

        ParsedItem schulter = finde(items, "Rindsschulter");
        assertThat(schulter.menge()).isEqualByComparingTo(new BigDecimal("4.4"));
        assertThat(schulter.einheit()).isEqualTo("kg");
        assertThat(schulter.kategorie()).isEqualTo("Fleisch");

        ParsedItem talg = finde(items, "Rindertalg");
        assertThat(talg.menge()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(talg.einheit()).isEqualTo("g");

        ParsedItem zwiebeln = finde(items, "Zwiebeln");
        assertThat(zwiebeln.menge()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(zwiebeln.einheit()).isEqualTo("Stück"); // woertlich

        // Rest der Zeile bleibt komplett Bezeichnung
        ParsedItem oregano = finde(items, "Oregano getrocknet");
        assertThat(oregano.einheit()).isEqualTo("Pack");
        assertThat(oregano.kategorie()).isEqualTo("Gewürze");

        assertThat(finde(items, "Knoblauch").einheit()).isEqualTo("Knolle");
        assertThat(finde(items, "Koriander").einheit()).isEqualTo("Bund");
        assertThat(finde(items, "Tomatenmark").einheit()).isEqualTo("Tube");
        assertThat(finde(items, "Vakuumbeutel gross").menge()).isEqualByComparingTo(new BigDecimal("4"));
    }

    @Test
    void ohneLeerzeichenAberOhneRestBleibtDasWortBezeichnung() {
        List<ParsedItem> items = parser.parse("- [ ] 500g");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).bezeichnung()).isEqualTo("g");
        assertThat(items.get(0).einheit()).isNull();
    }

}
