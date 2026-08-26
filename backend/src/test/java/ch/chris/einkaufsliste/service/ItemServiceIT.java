package ch.chris.einkaufsliste.service;

import ch.chris.einkaufsliste.domain.AbstractIntegrationTest;
import ch.chris.einkaufsliste.domain.entity.AppUser;
import ch.chris.einkaufsliste.domain.entity.Item;
import ch.chris.einkaufsliste.domain.entity.ShoppingList;
import ch.chris.einkaufsliste.domain.enums.ListStatus;
import ch.chris.einkaufsliste.domain.repository.AppUserRepository;
import ch.chris.einkaufsliste.domain.repository.ItemRepository;
import ch.chris.einkaufsliste.domain.repository.ShoppingListRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ItemServiceIT extends AbstractIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ListService listService;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private EntityManager entityManager;

    private ShoppingList list;

    private ShoppingList list() {
        if (list == null) {
            AppUser owner = appUserRepository.save(new AppUser("g-" + System.nanoTime(), "x@x.com", "X"));
            list = listService.create("Testliste", null, owner);
        }
        return list;
    }

    @Test
    void addLegtItemUnterDerListeAn() {
        Item item = itemService.add(list().getId(), "Tomaten", new BigDecimal("500"), "g", "Gemüse");

        entityManager.flush();
        entityManager.clear();

        assertThat(item.getId()).isNotNull();
        Item reloaded = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(reloaded.getBezeichnung()).isEqualTo("Tomaten");
        assertThat(reloaded.getKategorie()).isEqualTo("Gemüse");
    }

    @Test
    void updateAendertFelder() {
        Item item = itemService.add(list().getId(), "Milch", new BigDecimal("1"), "l", null);

        itemService.update(item.getId(), "Milch laktosefrei", new BigDecimal("2"), "l", "Milchprodukte");
        entityManager.flush();
        entityManager.clear();

        Item reloaded = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(reloaded.getBezeichnung()).isEqualTo("Milch laktosefrei");
        assertThat(reloaded.getMenge()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(reloaded.getKategorie()).isEqualTo("Milchprodukte");
    }

    @Test
    void deleteEntferntItemAusDerListe() {
        Item item = itemService.add(list().getId(), "Brot", new BigDecimal("1"), "Stk", null);
        Long listId = list().getId();

        itemService.delete(item.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(itemRepository.findById(item.getId())).isEmpty();
        assertThat(shoppingListRepository.findById(listId).orElseThrow().getItems()).isEmpty();
    }

    @Test
    void toggleAbgehaktSetztFlagUndZeitstempel() {
        Item item = itemService.add(list().getId(), "Käse", new BigDecimal("1"), "Stk", null);

        itemService.toggleAbgehakt(item.getId(), true);
        entityManager.flush();
        entityManager.clear();

        Item reloaded = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(reloaded.isAbgehakt()).isTrue();
        assertThat(reloaded.getAbgehaktAm()).isNotNull();
    }

    @Test
    void toggleAbgehaktArchiviertListeAutomatischWennDadurchAlleAbgehaktSind() {
        Item a = itemService.add(list().getId(), "Kaffee", new BigDecimal("1"), "Pkg", null);
        Item b = itemService.add(list().getId(), "Zucker", new BigDecimal("1"), "kg", null);
        Long listId = list().getId();

        itemService.toggleAbgehakt(a.getId(), true);
        entityManager.flush();
        entityManager.clear();
        assertThat(shoppingListRepository.findById(listId).orElseThrow().getStatus())
                .isEqualTo(ListStatus.AKTIV); // erst eins von zwei abgehakt

        itemService.toggleAbgehakt(b.getId(), true);
        entityManager.flush();
        entityManager.clear();
        assertThat(shoppingListRepository.findById(listId).orElseThrow().getStatus())
                .isEqualTo(ListStatus.ARCHIVIERT); // jetzt beide abgehakt -> Auto-Archivierung
    }

    @Test
    void toggleAbgehaktAufFalseArchiviertNie() {
        Item item = itemService.add(list().getId(), "Nudeln", new BigDecimal("500"), "g", null);
        Long listId = list().getId();

        itemService.toggleAbgehakt(item.getId(), false);
        entityManager.flush();
        entityManager.clear();

        assertThat(shoppingListRepository.findById(listId).orElseThrow().getStatus())
                .isEqualTo(ListStatus.AKTIV);
    }

    @Test
    void toggleAbgehaktAufFalseReaktiviertArchivierteListeAutomatischOhneAndereHakenZurueckzusetzen() {
        Item a = itemService.add(list().getId(), "Kaffee", new BigDecimal("1"), "Pkg", null);
        Item b = itemService.add(list().getId(), "Zucker", new BigDecimal("1"), "kg", null);
        Long listId = list().getId();

        itemService.toggleAbgehakt(a.getId(), true);
        itemService.toggleAbgehakt(b.getId(), true);
        entityManager.flush();
        entityManager.clear();
        assertThat(shoppingListRepository.findById(listId).orElseThrow().getStatus())
                .isEqualTo(ListStatus.ARCHIVIERT); // beide abgehakt -> automatisch archiviert

        // Nutzer hakt EIN Item wieder auf
        itemService.toggleAbgehakt(a.getId(), false);
        entityManager.flush();
        entityManager.clear();

        ShoppingList reloaded = shoppingListRepository.findById(listId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ListStatus.AKTIV); // automatisch reaktiviert
        assertThat(itemRepository.findById(a.getId()).orElseThrow().isAbgehakt()).isFalse();
        assertThat(itemRepository.findById(b.getId()).orElseThrow().isAbgehakt()).isTrue(); // NICHT zurueckgesetzt
    }

}
