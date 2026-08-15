package ch.chris.einkaufsliste.service;

import ch.chris.einkaufsliste.domain.entity.AppUser;
import ch.chris.einkaufsliste.domain.entity.ListMember;
import ch.chris.einkaufsliste.domain.entity.ShoppingList;
import ch.chris.einkaufsliste.domain.enums.SortField;
import ch.chris.einkaufsliste.domain.repository.ShoppingListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Orchestriert Anwendungsfaelle rund um ShoppingList. Die eigentliche
 * Business-Logik (Archivieren/Reaktivieren/"alle abgehakt?") liegt bewusst
 * in der ShoppingList-Entity selbst (reiches Domaenenmodell) - der Service
 * laedt/speichert und ruft die Entity-Methoden auf, statt die Logik hier zu
 * duplizieren.
 */
@Service
public class ListService {

    private final ShoppingListRepository shoppingListRepository;

    public ListService(ShoppingListRepository shoppingListRepository) {
        this.shoppingListRepository = shoppingListRepository;
    }

    @Transactional
    public ShoppingList create(String name, AppUser owner) {
        return shoppingListRepository.save(new ShoppingList(name, owner));
    }

    @Transactional
    public void archive(Long listId) {
        getOrThrow(listId).archive();
    }

    @Transactional
    public void reactivate(Long listId) {
        getOrThrow(listId).reactivate();
    }

    /**
     * Wird nach jedem Abhaken eines Items aufgerufen (siehe ItemService,
     * naechster Teilschritt). Archiviert automatisch, wenn dadurch alle
     * Items der Liste abgehakt sind - no-op sonst.
     */
    @Transactional
    public void archiveIfAllItemsChecked(Long listId) {
        ShoppingList list = getOrThrow(listId);
        if (list.areAllItemsChecked()) {
            list.archive();
        }
    }

    @Transactional
    public ListMember addMember(Long listId, AppUser user) {
        ShoppingList list = getOrThrow(listId);

        if (list.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Der Owner ist implizit bereits Mitglied der Liste");
        }
        // Bewusst ueber m.getUser().getId() statt m.getId().getUserId():
        // die zusammengesetzte @MapsId-ID wird von Hibernate erst beim
        // Flush aus der user-Referenz abgeleitet, ist also bei frisch
        // (im selben Transaktions-Durchlauf) hinzugefuegten, noch nicht
        // geflushten Members noch null.
        boolean bereitsMitglied = list.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(user.getId()));
        if (bereitsMitglied) {
            throw new IllegalArgumentException("User ist bereits Mitglied dieser Liste");
        }

        return list.addMember(user);
    }

    @Transactional
    public void removeMember(Long listId, Long userId) {
        ShoppingList list = getOrThrow(listId);
        ListMember member = list.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User ist kein Mitglied dieser Liste"));
        list.removeMember(member);
    }

    /**
     * Loest die effektive Sortierpraeferenz fuer einen User auf dieser Liste
     * auf: individuelles Member-Override falls gesetzt, sonst list.sortierung
     * (Default/Owner-Praeferenz).
     */
    @Transactional(readOnly = true)
    public SortField resolveSortField(Long listId, Long userId) {
        ShoppingList list = getOrThrow(listId);
        return list.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .map(ListMember::getSortierung)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(list.getSortierung());
    }

    private ShoppingList getOrThrow(Long listId) {
        return shoppingListRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Liste nicht gefunden: " + listId));
    }

}
