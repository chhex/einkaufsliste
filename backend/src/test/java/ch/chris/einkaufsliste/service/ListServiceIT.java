package ch.chris.einkaufsliste.service;

import ch.chris.einkaufsliste.domain.AbstractIntegrationTest;
import ch.chris.einkaufsliste.domain.entity.AppUser;
import ch.chris.einkaufsliste.domain.entity.Item;
import ch.chris.einkaufsliste.domain.entity.ListMember;
import ch.chris.einkaufsliste.domain.entity.ShoppingList;
import ch.chris.einkaufsliste.domain.enums.ListStatus;
import ch.chris.einkaufsliste.domain.enums.SortField;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @Transactional auf Testklassen-Ebene: haelt den gesamten Testfall in EINER
 * Transaktion/EINEM Persistence-Context (jeder Service-Aufruf innerhalb des
 * Tests joint diese Transaktion statt eine eigene zu oeffnen und wieder zu
 * schliessen) - vermeidet "detached entity" Fehler, wenn im selben Test
 * mehrfach mit demselben Objekt gearbeitet wird. Bonus: automatischer
 * Rollback nach jedem Test, kein manuelles Aufraeumen noetig.
 */
@SpringBootTest
@Transactional
class ListServiceIT extends AbstractIntegrationTest {

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

    private AppUser owner;
    private AppUser partner;

    private AppUser owner() {
        if (owner == null) {
            owner = appUserRepository.save(new AppUser("g-owner-" + System.nanoTime(), "owner@x.com", "Owner"));
        }
        return owner;
    }

    private AppUser partner() {
        if (partner == null) {
            partner = appUserRepository.save(new AppUser("g-partner-" + System.nanoTime(), "partner@x.com", "Partner"));
        }
        return partner;
    }

    @Test
    void createLegtNeueAktiveListeAn() {
        ShoppingList list = listService.create("Migros", null, owner());

        assertThat(list.getId()).isNotNull();
        assertThat(list.getStatus()).isEqualTo(ListStatus.AKTIV);
    }

    @Test
    void archiveSetztStatusUndZeitstempel() {
        ShoppingList list = listService.create("Bauhaus", null, owner());

        listService.archive(list.getId());
        entityManager.flush();
        entityManager.clear();

        ShoppingList reloaded = shoppingListRepository.findById(list.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ListStatus.ARCHIVIERT);
        assertThat(reloaded.getArchivedAt()).isNotNull();
    }

    @Test
    void reactivateSetztStatusZurueckUndResettetAlleHaken() {
        ShoppingList list = new ShoppingList("Coop", owner());
        Item a = list.addItem("Milch", new BigDecimal("1"), "l");
        Item b = list.addItem("Brot", new BigDecimal("1"), "Stk");
        a.setAbgehakt(true);
        b.setAbgehakt(true);
        list = shoppingListRepository.save(list);
        list.archive();
        entityManager.flush();
        entityManager.clear();

        listService.reactivate(list.getId());
        entityManager.flush();
        entityManager.clear();

        ShoppingList reloaded = shoppingListRepository.findById(list.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ListStatus.AKTIV);
        assertThat(reloaded.getArchivedAt()).isNull();
        assertThat(reloaded.getItems()).allMatch(item -> !item.isAbgehakt());
    }

    @Test
    void archiveIfAllItemsCheckedArchiviertNurWennWirklichAlleAbgehaktSind() {
        ShoppingList list = new ShoppingList("Denner", owner());
        Item a = list.addItem("Kaffee", new BigDecimal("1"), "Pkg");
        list.addItem("Zucker", new BigDecimal("1"), "kg"); // bleibt offen
        a.setAbgehakt(true);
        list = shoppingListRepository.save(list);
        entityManager.flush();

        listService.archiveIfAllItemsChecked(list.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(shoppingListRepository.findById(list.getId()).orElseThrow().getStatus())
                .isEqualTo(ListStatus.AKTIV); // noch nicht alle abgehakt

        Item zucker = itemRepository.findByListIdOrderByBezeichnungAsc(list.getId()).stream()
                .filter(i -> i.getBezeichnung().equals("Zucker"))
                .findFirst().orElseThrow();
        zucker.setAbgehakt(true);
        itemRepository.save(zucker);

        listService.archiveIfAllItemsChecked(list.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(shoppingListRepository.findById(list.getId()).orElseThrow().getStatus())
                .isEqualTo(ListStatus.ARCHIVIERT); // jetzt wirklich alle abgehakt
    }

    @Test
    void addMemberUndRemoveMemberFunktionieren() {
        ShoppingList list = listService.create("Gemeinsame Liste", null, owner());

        ListMember member = listService.addMember(list.getId(), partner().getId());
        assertThat(member.getUser().getName()).isEqualTo("Partner");

        listService.removeMember(list.getId(), partner().getId());
        entityManager.flush();
        entityManager.clear();

        ShoppingList reloaded = shoppingListRepository.findById(list.getId()).orElseThrow();
        assertThat(reloaded.getMembers()).isEmpty();
    }

    @Test
    void addMemberLehntOwnerAlsMitgliedAb() {
        ShoppingList list = listService.create("Test", null, owner());

        assertThatThrownBy(() -> listService.addMember(list.getId(), owner().getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveSortFieldNutztOverrideWennGesetztSonstListDefault() {
        ShoppingList list = listService.create("Sortier-Test", null, owner());
        // Owner selbst hat kein Member-Override -> Default der Liste
        assertThat(listService.resolveSortField(list.getId(), owner().getId()))
                .isEqualTo(SortField.KATEGORIE);

        ListMember member = listService.addMember(list.getId(), partner().getId());
        member.setSortierung(SortField.BEZEICHNUNG);
        entityManager.flush();
        entityManager.clear();

        assertThat(listService.resolveSortField(list.getId(), partner().getId()))
                .isEqualTo(SortField.BEZEICHNUNG);
    }

    @Test
    void deleteEntferntListeUndCascadedItems() {
        ShoppingList list = new ShoppingList("Zu loeschen", owner());
        list.addItem("Milch", new BigDecimal("1"), "l");
        list = shoppingListRepository.save(list);
        Long listId = list.getId();

        listService.delete(listId);
        entityManager.flush();
        entityManager.clear();

        assertThat(shoppingListRepository.findById(listId)).isEmpty();
    }

    @Test
    void deleteNichtExistierenderListeWirftFehler() {
        assertThatThrownBy(() -> listService.delete(999999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reactivateIfArchivedDueToItemUncheckReaktiviertNurEinmalArchivierte() {
        ShoppingList list = listService.create("Test", null, owner());
        listService.archive(list.getId());

        listService.reactivateIfArchivedDueToItemUncheck(list.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(shoppingListRepository.findById(list.getId()).orElseThrow().getStatus())
                .isEqualTo(ListStatus.AKTIV);
    }

}
