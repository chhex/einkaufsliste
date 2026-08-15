package ch.chris.einkaufsliste.domain;

import ch.chris.einkaufsliste.domain.entity.AppUser;
import ch.chris.einkaufsliste.domain.entity.Item;
import ch.chris.einkaufsliste.domain.entity.ListMember;
import ch.chris.einkaufsliste.domain.entity.ShoppingList;
import ch.chris.einkaufsliste.domain.enums.ListStatus;
import ch.chris.einkaufsliste.domain.enums.SortField;
import ch.chris.einkaufsliste.domain.repository.AppUserRepository;
import ch.chris.einkaufsliste.domain.repository.ItemRepository;
import ch.chris.einkaufsliste.domain.repository.ListMemberRepository;
import ch.chris.einkaufsliste.domain.repository.ShoppingListRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifiziert Schritt 3b: JPA-Entities + Repositories gegen das in 3a
 * angelegte Schema (echte Postgres via Testcontainers). ShoppingList ist
 * die Aggregat-Wurzel - Items/Members werden bewusst ueber
 * list.addItem()/addMember() erzeugt, nie direkt konstruiert.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ListMemberRepository listMemberRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void speichertUndLaedtEineEinkaufslisteMitItems() {
        AppUser owner = appUserRepository.save(new AppUser("google-1", "chris@example.com", "Chris"));

        ShoppingList list = new ShoppingList("Migros", owner);
        list.addItem("Tomaten", new BigDecimal("500"), "g").setKategorie("Gemüse");
        list.addItem("Bier", new BigDecimal("6"), "Stk").setKategorie("Getränke");

        list = shoppingListRepository.save(list);
        entityManager.flush();
        entityManager.clear();

        assertThat(list.getId()).isNotNull();
        assertThat(list.getStatus()).isEqualTo(ListStatus.AKTIV);
        assertThat(list.getSortierung()).isEqualTo(SortField.KATEGORIE);

        List<Item> items = itemRepository.findByListIdOrderByKategorieAscBezeichnungAsc(list.getId());
        assertThat(items).extracting(Item::getBezeichnung).containsExactly("Tomaten", "Bier");
    }

    @Test
    void abhakenSetztAbgehaktAmAutomatisch() {
        AppUser owner = appUserRepository.save(new AppUser("google-2", "owner2@example.com", "Owner2"));
        ShoppingList list = new ShoppingList("Bauhaus", owner);
        Item item = list.addItem("Schrauben", new BigDecimal("20"), "Stk");
        list = shoppingListRepository.save(list);

        assertThat(item.getId()).isNotNull(); // via Cascade beim Speichern der Liste vergeben
        assertThat(item.getAbgehaktAm()).isNull();

        item.setAbgehakt(true);
        itemRepository.save(item);

        Item reloaded = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(reloaded.isAbgehakt()).isTrue();
        assertThat(reloaded.getAbgehaktAm()).isNotNull();
    }

    @Test
    void listMemberKannIndividuelleSortierungUeberschreiben() {
        AppUser owner = appUserRepository.save(new AppUser("google-3", "owner3@example.com", "Owner3"));
        AppUser partner = appUserRepository.save(new AppUser("google-4", "partner@example.com", "Partner"));

        ShoppingList list = new ShoppingList("Gemeinsame Liste", owner);
        ListMember member = list.addMember(partner);
        member.setSortierung(SortField.BEZEICHNUNG);
        list = shoppingListRepository.save(list);

        ListMember reloaded = listMemberRepository.findById(member.getId()).orElseThrow();
        assertThat(reloaded.getSortierung()).isEqualTo(SortField.BEZEICHNUNG);
        assertThat(reloaded.getUser().getName()).isEqualTo("Partner");
        assertThat(list.getSortierung()).isEqualTo(SortField.KATEGORIE); // Owner-Default bleibt unangetastet
    }

    @Test
    void ungueltigerListStatusWirdVonDbConstraintAbgelehnt() {
        AppUser owner = appUserRepository.save(new AppUser("google-5", "owner5@example.com", "Owner5"));
        ShoppingList list = shoppingListRepository.save(new ShoppingList("Test", owner));
        entityManager.flush();

        // Bewusst am Enum vorbei, direkt per natives SQL - simuliert einen
        // ungueltigen Wert, wie ihn nur ein Bug oder externer Zugriff erzeugen koennte.
        assertThatThrownBy(() ->
                entityManager
                        .createNativeQuery("UPDATE list SET status = 'UNGUELTIG' WHERE id = :id")
                        .setParameter("id", list.getId())
                        .executeUpdate()
        ).isInstanceOf(RuntimeException.class);
    }

    @Test
    void findAccessibleByUserIdFindetOwnerUndMemberListen() {
        AppUser owner = appUserRepository.save(new AppUser("google-6", "owner6@example.com", "Owner6"));
        AppUser partner = appUserRepository.save(new AppUser("google-7", "partner7@example.com", "Partner7"));

        shoppingListRepository.save(new ShoppingList("Eigene Liste", owner));

        ShoppingList geteilteListe = new ShoppingList("Geteilte Liste", partner);
        geteilteListe.addMember(owner);
        shoppingListRepository.save(geteilteListe);

        List<ShoppingList> accessible = shoppingListRepository.findAccessibleByUserId(owner.getId());

        assertThat(accessible)
                .extracting(ShoppingList::getName)
                .containsExactlyInAnyOrder("Eigene Liste", "Geteilte Liste");
    }

}
