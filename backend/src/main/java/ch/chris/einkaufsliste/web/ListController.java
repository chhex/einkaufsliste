package ch.chris.einkaufsliste.web;

import ch.chris.einkaufsliste.domain.entity.AppUser;
import ch.chris.einkaufsliste.domain.entity.ListMember;
import ch.chris.einkaufsliste.domain.entity.ShoppingList;
import ch.chris.einkaufsliste.service.ListService;
import ch.chris.einkaufsliste.service.UserService;
import ch.chris.einkaufsliste.web.dto.AddMemberRequest;
import ch.chris.einkaufsliste.web.dto.CreateListRequest;
import ch.chris.einkaufsliste.web.dto.ListResponse;
import ch.chris.einkaufsliste.web.dto.MemberResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * TODO: "userId" kommt aktuell noch als Query-Parameter, weil Google-OAuth2
 * (SecurityConfig ist noch provisorisch offen) erst spaeter verdrahtet wird.
 * Sobald das steht, wird der User aus dem authentifizierten Principal
 * gelesen statt explizit uebergeben.
 */
@RestController
@RequestMapping("/api/lists")
public class ListController {

    private final ListService listService;
    private final UserService userService;

    public ListController(ListService listService, UserService userService) {
        this.listService = listService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ListResponse> create(@Valid @RequestBody CreateListRequest request) {
        AppUser owner = userService.get(request.ownerId());
        ShoppingList list = listService.create(request.name(), owner);
        return ResponseEntity.created(URI.create("/api/lists/" + list.getId()))
                .body(ListResponse.from(list));
    }

    @GetMapping
    public List<ListResponse> getAccessibleLists(@RequestParam Long userId) {
        return listService.getAccessibleByUser(userId).stream()
                .map(ListResponse::summary)
                .toList();
    }

    @GetMapping("/{id}")
    public ListResponse get(@PathVariable Long id) {
        return ListResponse.from(listService.get(id));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        listService.archive(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable Long id) {
        listService.reactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public MemberResponse addMember(@PathVariable Long id, @Valid @RequestBody AddMemberRequest request) {
        AppUser user = userService.get(request.userId());
        ListMember member = listService.addMember(id, user);
        return MemberResponse.from(member);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        listService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

}
