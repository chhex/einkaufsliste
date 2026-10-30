package ch.chris.einkaufsliste.service.importer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reiner Unit-Test: kein Spring-Context, kein Docker. Testet ImportService
 * isoliert - mit einem simplen Test-Double statt dem echten Parser,
 * damit der Test unabhaengig von dessen Parsing-Details bleibt.
 */
class ImportServiceTest {

    private static final class StubParser implements GroceryListParser {
        @Override
        public ImportSource getSource() {
            return ImportSource.DELIMITED_TEXT;
        }

        @Override
        public List<ParsedItem> parse(String rawText) {
            return List.of(new ParsedItem(rawText, BigDecimal.ONE, "Stk", null));
        }
    }

    @Test
    void delegiertAnDenPasendenParserFuerDieAngegebeneQuelle() {
        ImportService importService = new ImportService(List.of(new StubParser()));

        List<ParsedItem> result = importService.parse(ImportSource.DELIMITED_TEXT, "Testzeile");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bezeichnung()).isEqualTo("Testzeile");
    }

    @Test
    void wirftFehlerWennKeinParserFuerDieQuelleRegistriertIst() {
        ImportService importService = new ImportService(List.of()); // keine Parser registriert

        assertThatThrownBy(() -> importService.parse(ImportSource.DELIMITED_TEXT, "egal"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DELIMITED_TEXT");
    }

    @Test
    void nutztDenEchtenDelimitedTextParserUeberDieOeffentlicheApi() {
        // Integration der beiden Klassen ohne Spring-Context - stellt sicher,
        // dass ImportService + DelimitedTextParser tatsaechlich zusammenspielen.
        ImportService importService = new ImportService(List.of(new DelimitedTextParser()));

        List<ParsedItem> result = importService.parse(ImportSource.DELIMITED_TEXT,
                "2kg / Mehl / Backen; Milch");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).bezeichnung()).isEqualTo("Mehl");
        assertThat(result.get(0).einheit()).isEqualTo("kg");
        assertThat(result.get(0).kategorie()).isEqualTo("Backen");
        assertThat(result.get(0).menge()).isEqualByComparingTo(new java.math.BigDecimal("2"));
        assertThat(result.get(1).bezeichnung()).isEqualTo("Milch");
    }

}
