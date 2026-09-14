package ch.chris.einkaufsliste.web;

import ch.chris.einkaufsliste.domain.entity.Item;
import ch.chris.einkaufsliste.service.ItemService;
import ch.chris.einkaufsliste.service.ListService;
import ch.chris.einkaufsliste.web.dto.ItemRequest;
import ch.chris.einkaufsliste.web.dto.ItemResponse;
import ch.chris.einkaufsliste.web.dto.ToggleAbgehaktRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Autorisierung (siehe ListController fuer die generelle Erklaerung):
 * Items haben keinen eigenen Owner-Begriff, ihre Berechtigung leitet sich
 * immer aus der zugehoerigen Liste ab. Bei add() ist die Listen-ID direkt
 * im Pfad vorhanden (listService.assertAccess); bei den anderen Endpoints
 * (nur Item-ID im Pfad) loest itemService.assertAccess die Liste erst ueber
 * das Item auf.
 */
@RestController
public class ItemController {

    private final ItemService itemService;
    private final ListService listService;

    public ItemController(ItemService itemService, ListService listService) {
        this.itemService = itemService;
        this.listService = listService;
    }

    @PostMapping("/api/lists/{listId}/items")
    public ResponseEntity<ItemResponse> add(@AuthenticationPrincipal Long userId,
                                             @PathVariable Long listId,
                                             @Valid @RequestBody ItemRequest request) {
        listService.assertAccess(listId, userId);
        Item item = itemService.add(listId, request.bezeichnung(), request.menge(), request.einheit(), request.kategorie());
        return ResponseEntity.created(URI.create("/api/items/" + item.getId()))
                .body(ItemResponse.from(item));
    }

    @PutMapping("/api/items/{id}")
    public ResponseEntity<Void> update(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long id,
                                        @Valid @RequestBody ItemRequest request) {
        itemService.assertAccess(id, userId);
        itemService.update(id, request.bezeichnung(), request.menge(), request.einheit(), request.kategorie());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/items/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        itemService.assertAccess(id, userId);
        itemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/items/{id}/abgehakt")
    public ResponseEntity<Void> toggleAbgehakt(@AuthenticationPrincipal Long userId,
                                                @PathVariable Long id,
                                                @RequestBody ToggleAbgehaktRequest request) {
        itemService.assertAccess(id, userId);
        itemService.toggleAbgehakt(id, request.abgehakt());
        return ResponseEntity.noContent().build();
    }

}
