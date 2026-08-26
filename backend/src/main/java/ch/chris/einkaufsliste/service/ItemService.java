package ch.chris.einkaufsliste.service;

import ch.chris.einkaufsliste.domain.entity.Item;
import ch.chris.einkaufsliste.domain.entity.ShoppingList;
import ch.chris.einkaufsliste.domain.repository.ItemRepository;
import ch.chris.einkaufsliste.domain.repository.ShoppingListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Orchestriert Anwendungsfaelle rund um Item. Items werden ausschliesslich
 * ueber die Aggregat-Wurzel (ShoppingList.addItem()/removeItem()) erzeugt
 * bzw. entfernt - konsistent mit dem package-private Item-Konstruktor.
 */
@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final ListService listService;

    public ItemService(ItemRepository itemRepository,
                        ShoppingListRepository shoppingListRepository,
                        ListService listService) {
        this.itemRepository = itemRepository;
        this.shoppingListRepository = shoppingListRepository;
        this.listService = listService;
    }

    @Transactional
    public Item add(Long listId, String bezeichnung, BigDecimal menge, String einheit, String kategorie) {
        ShoppingList list = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Liste nicht gefunden: " + listId));
        Item item = list.addItem(bezeichnung, menge, einheit);
        item.setKategorie(kategorie);
        // Explizit speichern statt nur auf die Cascade zu vertrauen: bei
        // GenerationType.IDENTITY erzwingt das ein sofortiges INSERT, sodass
        // item.getId() garantiert gesetzt ist, wenn diese Methode zurueckkehrt
        // (wichtig fuer Aufrufer, die die ID sofort brauchen, z.B. spaeter
        // der REST-Controller fuer den Location-Header der 201-Response).
        return itemRepository.save(item);
    }

    @Transactional
    public void update(Long itemId, String bezeichnung, BigDecimal menge, String einheit, String kategorie) {
        Item item = getOrThrow(itemId);
        item.setBezeichnung(bezeichnung);
        item.setMenge(menge);
        item.setEinheit(einheit);
        item.setKategorie(kategorie);
    }

    @Transactional
    public void delete(Long itemId) {
        Item item = getOrThrow(itemId);
        item.getList().removeItem(item);
        // Zusaetzlich explizit loeschen statt sich allein auf orphanRemoval
        // zu verlassen: bei mappedBy-Collections, die ueber eine "Umweg"-
        // Referenz (hier item.getList()) statt einer frisch vom Repository
        // geholten Liste mutiert werden, kann Hibernates Dirty-Checking das
        // Entfernen unzuverlaessig erkennen. Explizites delete() garantiert
        // das DELETE unabhaengig vom Cascade-Timing.
        itemRepository.delete(item);
    }

    /**
     * Hakt ein Item ab bzw. wieder auf.
     * - Abhaken (abgehakt=true): prueft anschliessend, ob dadurch ALLE Items
     *   der Liste abgehakt sind - falls ja, archiviert ListService die Liste
     *   automatisch.
     * - Wiederaufhaken (abgehakt=false): falls die Liste bereits ARCHIVIERT
     *   war (widersprueclicher Zustand sonst: archivierte Liste mit offenem
     *   Item), wird sie automatisch reaktiviert - OHNE die anderen Haken
     *   zurueckzusetzen (andere Semantik als der explizite
     *   "Reaktivieren"-Button, siehe ShoppingList.unarchiveDueToItemUncheck).
     */
    @Transactional
    public void toggleAbgehakt(Long itemId, boolean abgehakt) {
        Item item = getOrThrow(itemId);
        item.setAbgehakt(abgehakt);
        Long listId = item.getList().getId();
        if (abgehakt) {
            listService.archiveIfAllItemsChecked(listId);
        } else {
            listService.reactivateIfArchivedDueToItemUncheck(listId);
        }
    }

    private Item getOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item nicht gefunden: " + itemId));
    }

}
