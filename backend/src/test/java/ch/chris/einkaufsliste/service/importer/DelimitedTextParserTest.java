package ch.chris.einkaufsliste.service.importer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reiner Unit-Test: kein Spring-Context, kein Docker.
 */
class DelimitedTextParserTest {

    private final DelimitedTextParser parser = new DelimitedTextParser();

    private ParsedItem einzeln(String text) {
        List<ParsedItem> items = parser.parse(text);
        assertThat(items).hasSize(1);
        return items.get(0);
    }

    @Test
    void getSourceLiefertDelimitedText() {
        assertThat(parser.getSource()).isEqualTo(ImportSource.DELIMITED_TEXT);
    }

    @Test
    void dreiFelderMengeBezeichnungKategorie() {
        ParsedItem item = einzeln("1kg / Rindfleischschulter / Fleisch");

        assertThat(item.menge()).isEqualByComparingTo(new BigDecimal("1"));
        assertThat(item.einheit()).isEqualTo("kg");
        assertThat(item.bezeichnung()).isEqualTo("Rindfleischschulter");
        assertThat(item.kategorie()).isEqualTo("Fleisch");
    }

    @Test
    void zweiFelderMitMengeAmAnfangSindMengeUndBezeichnung() {
        ParsedItem item = einzeln("1kg / Rindfleischschulter");

        assertThat(item.menge()).isEqualByComparingTo(new BigDecimal("1"));
        assertThat(item.einheit()).isEqualTo("kg");
        assertThat(item.bezeichnung()).isEqualTo("Rindfleischschulter");
        assertThat(item.kategorie()).isNull();
    }

    @Test
    void zweiFelderOhneMengeSindBezeichnungUndKategorie() {
        ParsedItem item = einzeln("Apfelsaft / Getränke");

        assertThat(item.menge()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(item.einheit()).isNull();
        assertThat(item.bezeichnung()).isEqualTo("Apfelsaft");
        assertThat(item.kategorie()).isEqualTo("Getränke");
    }

    @Test
    void zweiFelderMitMengeUndNameImErstenFeldSindNameUndKategorie() {
        ParsedItem item = einzeln("2Stück Zwiebeln / Gemüse");

        assertThat(item.menge()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(item.einheit()).isEqualTo("Stück");
        assertThat(item.bezeichnung()).isEqualTo("Zwiebeln");
        assertThat(item.kategorie()).isEqualTo("Gemüse");
    }

    @Test
    void zweiFelderZahlOhneEinheitIstMenge() {
        ParsedItem item = einzeln("3 / Eier");

        assertThat(item.menge()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(item.einheit()).isNull();
        assertThat(item.bezeichnung()).isEqualTo("Eier");
    }

    @Test
    void ohneTrennerNurMengeUndEinheitAmAnfangKeineKategorie() {
        ParsedItem item = einzeln("1Liter Apfelsaft");

        assertThat(item.menge()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(item.einheit()).isEqualTo("Liter");
        assertThat(item.bezeichnung()).isEqualTo("Apfelsaft");
        assertThat(item.kategorie()).isNull();
    }

    @Test
    void nurBezeichnungBekommtMengeEins() {
        ParsedItem item = einzeln("Milch");

        assertThat(item.menge()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(item.bezeichnung()).isEqualTo("Milch");
        assertThat(item.kategorie()).isNull();
    }

    @Test
    void semikolonUndZeilenumbruchBeendenEintraege() {
        List<ParsedItem> items = parser.parse("1kg / Rindfleisch / Fleisch ; 1Liter / Apfelsaft / Getränke\nBrot;;\n");

        assertThat(items).extracting(ParsedItem::bezeichnung)
                .containsExactly("Rindfleisch", "Apfelsaft", "Brot");
    }

    @Test
    void trennerDuerfenGemischtWerden() {
        List<ParsedItem> items = parser.parse("""
                1kg / Rindfleisch ? Fleisch
                2Stück - Zwiebeln - Gemüse
                500g Butter / Milchprodukte
                """);

        assertThat(items).hasSize(3);
        assertThat(items.get(0).kategorie()).isEqualTo("Fleisch");
        assertThat(items.get(1).bezeichnung()).isEqualTo("Zwiebeln");
        assertThat(items.get(1).kategorie()).isEqualTo("Gemüse");
        assertThat(items.get(2).einheit()).isEqualTo("g");
        assertThat(items.get(2).bezeichnung()).isEqualTo("Butter");
        assertThat(items.get(2).kategorie()).isEqualTo("Milchprodukte");
    }

    @Test
    void bindestrichIstNurMitLeerzeichenAufBeidenSeitenEinTrenner() {
        ParsedItem item = einzeln("Coca-Cola");

        assertThat(item.bezeichnung()).isEqualTo("Coca-Cola");
        assertThat(item.kategorie()).isNull();
    }

    @Test
    void kommaUndPunktSindKeineTrenner() {
        ParsedItem item = einzeln("1,5l / Äpfel, rot / Obst");

        assertThat(item.menge()).isEqualByComparingTo(new BigDecimal("1.5"));
        assertThat(item.einheit()).isEqualTo("l");
        assertThat(item.bezeichnung()).isEqualTo("Äpfel, rot");
        assertThat(item.kategorie()).isEqualTo("Obst");
    }

    @Test
    void schraegstrichZwischenZiffernIstKeinTrenner() {
        ParsedItem item = einzeln("1/2 Wassermelone");

        assertThat(item.bezeichnung()).contains("Wassermelone");
        assertThat(item.kategorie()).isNull();
    }

    @Test
    void leereEingabeLiefertLeereListe() {
        assertThat(parser.parse("  \n ; \n")).isEmpty();
    }

}
