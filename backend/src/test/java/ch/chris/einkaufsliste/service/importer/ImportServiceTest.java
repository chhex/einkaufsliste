package ch.chris.einkaufsliste.service.importer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reiner Unit-Test: kein Spring-Context, kein Docker. Testet ImportService
 * isoliert - mit einem simplen Test-Double statt dem echten NytCookingParser,
 * damit der Test unabhaengig von dessen Parsing-Details bleibt (die hat
 * NytCookingParserTest schon abgedeckt).
 */
class ImportServiceTest {

    private static final class StubParser implements GroceryListParser {
        @Override
        public ImportSource getSource() {
            return ImportSource.NYT_COOKING;
        }

        @Override
        public List<ParsedItem> parse(String rawText) {
            return List.of(new ParsedItem(rawText, BigDecimal.ONE, "Stk"));
        }
    }

    @Test
    void delegiertAnDenPasendenParserFuerDieAngegebeneQuelle() {
        ImportService importService = new ImportService(List.of(new StubParser()));

        List<ParsedItem> result = importService.parse(ImportSource.NYT_COOKING, "Testzeile");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bezeichnung()).isEqualTo("Testzeile");
    }

    @Test
    void wirftFehlerWennKeinParserFuerDieQuelleRegistriertIst() {
        ImportService importService = new ImportService(List.of()); // keine Parser registriert

        assertThatThrownBy(() -> importService.parse(ImportSource.NYT_COOKING, "egal"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NYT_COOKING");
    }

    @Test
    void nutztDenEchtenNytCookingParserUeberDieOeffentlicheApi() {
        // Integration der beiden Klassen ohne Spring-Context - stellt sicher,
        // dass ImportService + NytCookingParser tatsaechlich zusammenspielen,
        // nicht nur je isoliert fuer sich funktionieren.
        ImportService importService = new ImportService(List.of(new NytCookingParser()));

        List<ParsedItem> result = importService.parse(ImportSource.NYT_COOKING, """
                Titel
                1 Portion
                -
                2 cups Mehl
                ----------
                Footer
                """);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bezeichnung()).isEqualTo("Mehl");
        assertThat(result.get(0).einheit()).isEqualTo("cups");
    }

}
