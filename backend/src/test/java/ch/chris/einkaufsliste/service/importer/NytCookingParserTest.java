package ch.chris.einkaufsliste.service.importer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reiner Unit-Test: kein Spring-Context, kein Docker - reine Textverarbeitung.
 */
class NytCookingParserTest {

    private final NytCookingParser parser = new NytCookingParser();

    private static final String BEISPIEL_REZEPT = """
            Your Grocery List
            1 Recipe

            Skillet Meatballs With Peaches, Basil and Lime
            3 to 4 servings
            -
            1½ tablespoons finely grated or minced fresh ginger
            3 garlic cloves, grated or minced
            1¼ teaspoon ground cumin, plus more for serving
            1¼ teaspoons kosher salt, plus more as needed
            1 pound ground pork (or turkey or chicken, or vegan meat)
            ⅓ cup panko or other plain bread crumbs
            3 tablespoons finely chopped fresh basil, plus basil leaves for serving
            2 tablespoons extra-virgin olive oil
            2 tablespoons wine (dry white, rosé or red), or use broth, orange juice or water
            2 cups diced ripe peaches or nectarines (about 3)
            ¼ cup thinly sliced white or red onion, or scallions
            1 lime, halved
            White rice or coconut rice, rice noodles, or crisp salad greens, for serving

            ----------

            View Recipes on NYT Cooking
            -
            Skillet Meatballs With Peaches, Basil and Lime:
            https://cooking.nytimes.com/recipes/1021402-skillet-meatballs-with-peaches-basil-and-lime?smid=ck-grocery-list-ios
            """;

    @Test
    void getSourceLiefertNytCooking() {
        assertThat(parser.getSource()).isEqualTo(ImportSource.NYT_COOKING);
    }

    @Test
    void ignoriertKopfzeileMitRezeptnameUndPortionen() {
        List<ParsedItem> items = parser.parse(BEISPIEL_REZEPT);

        assertThat(items).noneMatch(i -> i.bezeichnung().contains("servings"));
        assertThat(items).noneMatch(i -> i.bezeichnung().contains("Skillet Meatballs"));
    }

    @Test
    void ignoriertFooterMitLinkUndRezeptname() {
        List<ParsedItem> items = parser.parse(BEISPIEL_REZEPT);

        assertThat(items).noneMatch(i -> i.bezeichnung().contains("cooking.nytimes.com"));
        assertThat(items).noneMatch(i -> i.bezeichnung().contains("View Recipes"));
    }

    @Test
    void erkenntErwarteteAnzahlZutaten() {
        List<ParsedItem> items = parser.parse(BEISPIEL_REZEPT);

        assertThat(items).hasSize(13);
    }

    @Test
    void konvertiertUnicodeBruchZuDezimalzahlUndEinheitBleibtLeer() {
        List<ParsedItem> items = parser.parse(BEISPIEL_REZEPT);

        ParsedItem ginger = items.stream()
                .filter(i -> i.bezeichnung().contains("ginger"))
                .findFirst().orElseThrow();
        assertThat(ginger.menge()).isEqualByComparingTo(new BigDecimal("1.5"));
        // Einheit wird bewusst NICHT mehr geraten - "tablespoons" bleibt
        // Teil der Bezeichnung, einheit() ist null.
        assertThat(ginger.einheit()).isNull();
        assertThat(ginger.bezeichnung()).startsWith("tablespoons");
    }

    @Test
    void kompletterRestBleibtBezeichnungOhneEinheitAbzutrennen() {
        List<ParsedItem> items = parser.parse(BEISPIEL_REZEPT);

        ParsedItem panko = items.stream()
                .filter(i -> i.bezeichnung().contains("panko"))
                .findFirst().orElseThrow();
        assertThat(panko.menge()).isEqualByComparingTo(new BigDecimal("0.3333"));
        assertThat(panko.einheit()).isNull();
        assertThat(panko.bezeichnung()).isEqualTo("cup panko or other plain bread crumbs");
    }

    @Test
    void garlicZeileBleibtVollstaendigAlsBezeichnungOhneEinheit() {
        List<ParsedItem> items = parser.parse(BEISPIEL_REZEPT);

        ParsedItem garlic = items.stream()
                .filter(i -> i.bezeichnung().contains("garlic"))
                .findFirst().orElseThrow();
        assertThat(garlic.einheit()).isNull();
        assertThat(garlic.menge()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(garlic.bezeichnung()).isEqualTo("garlic cloves, grated or minced");
    }

    @Test
    void zeileOhneErkennbareMengeBekommtDefaultMengeUndKeineEinheit() {
        List<ParsedItem> items = parser.parse(BEISPIEL_REZEPT);

        ParsedItem rice = items.stream()
                .filter(i -> i.bezeichnung().startsWith("White rice"))
                .findFirst().orElseThrow();
        assertThat(rice.menge()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(rice.einheit()).isNull();
    }

    @Test
    void unbekanntesFormatOhneMarkerLiefertLeereListe() {
        List<ParsedItem> items = parser.parse("Irgendein Text ohne die erwarteten Marker");

        assertThat(items).isEmpty();
    }

    @Test
    void bereichsangabeNimmtDenHoeherenWert() {
        String text = """
                Titel
                1 Portion
                -
                3 to 4 Tomaten
                2-3 Zwiebeln
                ----------
                Footer
                """;

        List<ParsedItem> items = parser.parse(text);

        ParsedItem tomaten = items.stream()
                .filter(i -> i.bezeichnung().contains("Tomaten"))
                .findFirst().orElseThrow();
        assertThat(tomaten.menge()).isEqualByComparingTo(new BigDecimal("4"));

        ParsedItem zwiebeln = items.stream()
                .filter(i -> i.bezeichnung().contains("Zwiebeln"))
                .findFirst().orElseThrow();
        assertThat(zwiebeln.menge()).isEqualByComparingTo(new BigDecimal("3"));
    }

}
