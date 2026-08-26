package ch.chris.einkaufsliste.domain.entity;

import ch.chris.einkaufsliste.domain.enums.ListStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Echter Unit-Test: reine Java-Objekte, KEIN Spring-Context, KEINE
 * Datenbank/Docker noetig. Testet die Business-Logik, die bewusst in der
 * Aggregat-Wurzel selbst liegt (siehe ShoppingList-Klassenkommentar).
 * Laeuft bei "mvn test" (Surefire, Standard-Namensmuster "*Test") -
 * im Gegensatz zu den *IT-Tests (Testcontainers), die "mvn verify" brauchen.
 */
class ShoppingListTest {

    private final AppUser owner = new AppUser("google-1", "chris@example.com", "Chris");
    private final AppUser partner = new AppUser("google-2", "partner@example.com", "Partner");

    @Test
    void neueListeIstAktivMitKategorieAlsDefaultSortierung() {
        ShoppingList list = new ShoppingList("Migros", owner);

        assertThat(list.getStatus()).isEqualTo(ListStatus.AKTIV);
        assertThat(list.getSortierung().name()).isEqualTo("KATEGORIE");
    }

    @Test
    void addItemFuegtItemHinzuUndSetztRueckreferenz() {
        ShoppingList list = new ShoppingList("Migros", owner);

        Item item = list.addItem("Tomaten", new BigDecimal("500"), "g");

        assertThat(list.getItems()).containsExactly(item);
        assertThat(item.getList()).isSameAs(list);
    }

    @Test
    void archiveSetztStatusUndZeitstempel() {
        ShoppingList list = new ShoppingList("Migros", owner);

        list.archive();

        assertThat(list.getStatus()).isEqualTo(ListStatus.ARCHIVIERT);
        assertThat(list.getArchivedAt()).isNotNull();
    }

    @Test
    void reactivateSetztStatusZurueckUndResettetAlleHaken() {
        ShoppingList list = new ShoppingList("Migros", owner);
        Item a = list.addItem("Milch", new BigDecimal("1"), "l");
        Item b = list.addItem("Brot", new BigDecimal("1"), "Stk");
        a.setAbgehakt(true);
        b.setAbgehakt(true);
        list.archive();

        list.reactivate();

        assertThat(list.getStatus()).isEqualTo(ListStatus.AKTIV);
        assertThat(list.getArchivedAt()).isNull();
        assertThat(a.isAbgehakt()).isFalse();
        assertThat(b.isAbgehakt()).isFalse();
        assertThat(a.getAbgehaktAm()).isNull();
    }

    @Test
    void unarchiveDueToItemUncheckReaktiviertOhneAndereHakenZurueckzusetzenUndAktualisiertDatum() {
        ShoppingList list = new ShoppingList("Migros", owner, LocalDate.of(2020, 1, 1));
        Item a = list.addItem("Milch", new BigDecimal("1"), "l");
        Item b = list.addItem("Brot", new BigDecimal("1"), "Stk");
        a.setAbgehakt(true);
        b.setAbgehakt(true);
        list.archive();

        // Nutzer hakt EIN Item wieder auf (Aufrufer setzt das vorher, wie
        // ItemService.toggleAbgehakt es tut) und ruft dann diese Methode auf
        b.setAbgehakt(false);
        list.unarchiveDueToItemUncheck();

        assertThat(list.getStatus()).isEqualTo(ListStatus.AKTIV);
        assertThat(list.getArchivedAt()).isNull();
        assertThat(list.getEinkaufsdatum()).isEqualTo(LocalDate.now());
        assertThat(a.isAbgehakt()).isTrue(); // NICHT zurueckgesetzt, im Gegensatz zu reactivate()
        assertThat(b.isAbgehakt()).isFalse();
    }

    @Test
    void unarchiveDueToItemUncheckIstNoOpWennListeBereitsAktiv() {
        ShoppingList list = new ShoppingList("Migros", owner, LocalDate.of(2020, 1, 1));

        list.unarchiveDueToItemUncheck();

        assertThat(list.getStatus()).isEqualTo(ListStatus.AKTIV);
        assertThat(list.getEinkaufsdatum()).isEqualTo(LocalDate.of(2020, 1, 1)); // unveraendert
    }

    @Test
    void areAllItemsCheckedIstFalseBeiLeererListe() {
        ShoppingList list = new ShoppingList("Migros", owner);

        assertThat(list.areAllItemsChecked()).isFalse();
    }

    @Test
    void areAllItemsCheckedIstFalseSolangeMindestensEinsOffenIst() {
        ShoppingList list = new ShoppingList("Migros", owner);
        list.addItem("Milch", new BigDecimal("1"), "l").setAbgehakt(true);
        list.addItem("Brot", new BigDecimal("1"), "Stk"); // bleibt offen

        assertThat(list.areAllItemsChecked()).isFalse();
    }

    @Test
    void areAllItemsCheckedIstTrueWennWirklichAlleAbgehaktSind() {
        ShoppingList list = new ShoppingList("Migros", owner);
        list.addItem("Milch", new BigDecimal("1"), "l").setAbgehakt(true);
        list.addItem("Brot", new BigDecimal("1"), "Stk").setAbgehakt(true);

        assertThat(list.areAllItemsChecked()).isTrue();
    }

    @Test
    void addMemberUndRemoveMemberFunktionieren() {
        ShoppingList list = new ShoppingList("Migros", owner);

        ListMember member = list.addMember(partner);
        assertThat(list.getMembers()).containsExactly(member);
        assertThat(member.getUser()).isSameAs(partner);

        list.removeMember(member);
        assertThat(list.getMembers()).isEmpty();
    }

}
