package ch.chris.einkaufsliste.web;

import ch.chris.einkaufsliste.domain.entity.Item;
import ch.chris.einkaufsliste.service.ItemService;
import ch.chris.einkaufsliste.web.dto.ItemRequest;
import ch.chris.einkaufsliste.web.dto.ItemResponse;
import ch.chris.einkaufsliste.web.dto.ToggleAbgehaktRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping("/api/lists/{listId}/items")
    public ResponseEntity<ItemResponse> add(@PathVariable Long listId, @Valid @RequestBody ItemRequest request) {
        Item item = itemService.add(listId, request.bezeichnung(), request.menge(), request.einheit(), request.kategorie());
        return ResponseEntity.created(URI.create("/api/items/" + item.getId()))
                .body(ItemResponse.from(item));
    }

    @PutMapping("/api/items/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
        itemService.update(id, request.bezeichnung(), request.menge(), request.einheit(), request.kategorie());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/items/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/items/{id}/abgehakt")
    public ResponseEntity<Void> toggleAbgehakt(@PathVariable Long id, @RequestBody ToggleAbgehaktRequest request) {
        itemService.toggleAbgehakt(id, request.abgehakt());
        return ResponseEntity.noContent().build();
    }

}
