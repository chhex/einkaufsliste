package ch.chris.einkaufsliste.service;

import ch.chris.einkaufsliste.domain.entity.AppUser;
import ch.chris.einkaufsliste.domain.entity.ListMember;
import ch.chris.einkaufsliste.domain.entity.ShoppingList;
import ch.chris.einkaufsliste.domain.enums.SortField;
import ch.chris.einkaufsliste.domain.repository.AppUserRepository;
import ch.chris.einkaufsliste.domain.repository.ShoppingListRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
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
    private final AppUserRepository appUserRepository;

    public ListService(ShoppingListRepository shoppingListRepository, AppUserRepository appUserRepository) {
        this.shoppingListRepository = shoppingListRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public ShoppingList create(String name, LocalDate einkaufsdatum, AppUser owner) {
        return shoppingListRepository.save(new ShoppingList(name, owner, einkaufsdatum));
    }

    /**
     * Name und/oder Einkaufsdatum nachtraeglich aendern - beide Parameter
     * optional, null bedeutet "unveraendert lassen".
     */
    @Transactional
    public void update(Long listId, String name, LocalDate einkaufsdatum) {
        ShoppingList list = getOrThrow(listId);
        if (name != null) {
            list.setName(name);
        }
        if (einkaufsdatum != null) {
            list.setEinkaufsdatum(einkaufsdatum);
        }
    }

    @Transactional(readOnly = true)
    public ShoppingList get(Long listId) {
        ShoppingList list = getOrThrow(listId);
        initializeForResponse(list);
        return list;
    }

    /**
     * Alle Listen, auf die der User Zugriff hat (Owner ODER Member).
     */
    @Transactional(readOnly = true)
    public List<ShoppingList> getAccessibleByUser(Long userId) {
        List<ShoppingList> lists = shoppingListRepository.findAccessibleByUserId(userId);
        lists.forEach(list -> Hibernate.initialize(list.getOwner()));
        return lists;
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
     * Hartes Loeschen einer Liste - im Gegensatz zu Archivieren/Reaktivieren
     * bewusst nur als EXPLIZITE Nutzeraktion (mit Bestaetigung im Frontend),
     * nicht automatisch. Cascade in der DB raeumt Items/Members mit auf.
     */
    @Transactional
    public void delete(Long listId) {
        if (!shoppingListRepository.existsById(listId)) {
            throw new IllegalArgumentException("Liste nicht gefunden: " + listId);
        }
        shoppingListRepository.deleteById(listId);
    }

    /**
     * Wird nach jedem Aufhaken (abgehakt=false) eines Items aufgerufen.
     * Falls die Liste ARCHIVIERT war, wird sie automatisch reaktiviert -
     * ohne die anderen Haken zurueckzusetzen (siehe
     * ShoppingList.unarchiveDueToItemUncheck). No-op, falls die Liste
     * bereits AKTIV ist.
     */
    @Transactional
    public void reactivateIfArchivedDueToItemUncheck(Long listId) {
        getOrThrow(listId).unarchiveDueToItemUncheck();
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

    /**
     * Nimmt bewusst nur die userId entgegen (nicht das AppUser-Objekt) und
     * laedt es hier selbst frisch - der Aufrufer (Controller) hat den User
     * u.U. in einer ANDEREN, bereits geschlossenen Transaktion geladen
     * (z.B. via UserService.getByEmail); ein solches "losgeloestes"
     * Objekt hier weiterzureichen fuehrt bei @MapsId-Assoziationen (siehe
     * ListMember) zu "detached entity passed to persist".
     */
    @Transactional
    public ListMember addMember(Long listId, Long userId) {
        ShoppingList list = getOrThrow(listId);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User nicht gefunden: " + userId));

        if (list.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Der Owner ist implizit bereits Mitglied der Liste");
        }
        // Bewusst ueber m.getUser().getId() statt m.getId().getUserId():
        // die zusammengesetzte @MapsId-ID wird von Hibernate erst beim
        // Flush aus der user-Referenz abgeleitet, ist also bei frisch
        // (im selben Transaktions-Durchlauf) hinzugefuegten, noch nicht
        // geflushten Members noch null.
        boolean bereitsMitglied = list.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userId));
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

    /**
     * Laedt alle lazy-Referenzen, die ListResponse.from() spaeter braucht,
     * WAEHREND die Transaktion/Session noch offen ist. Notwendig, weil
     * open-in-view=false ist (bewusst, siehe application.yml) - ohne das
     * hier wuerde der Controller nach Rueckkehr dieser Methode versuchen,
     * auf eine bereits geschlossene Hibernate-Session zuzugreifen
     * (LazyInitializationException). MockMvc-Tests (eine Transaktion pro
     * Testmethode) decken diesen Fall NICHT ab - deshalb ist der Bug dort
     * nicht aufgefallen, sondern erst im echten Betrieb (separate
     * Transaktion pro HTTP-Request).
     */
    private void initializeForResponse(ShoppingList list) {
        Hibernate.initialize(list.getOwner());
        list.getItems(); // laedt die Items-Collection
        for (ListMember member : list.getMembers()) {
            Hibernate.initialize(member.getUser());
        }
    }

}
