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
import ch.chris.einkaufsliste.web.dto.UpdateListRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * "userId" kommt hier NIE mehr vom Client (Query/Body) fuer die Frage "wer
 * bin ich" - immer ueber @AuthenticationPrincipal aus dem verifizierten JWT
 * (siehe JwtAuthenticationFilter). Waere es weiterhin ein Client-Parameter,
 * koennte sich ein eingeloggter User als beliebiger anderer User ausgeben.
 * <p>
 * TODO (Autorisierungs-Luecke, noch offen): aktuell kann JEDER eingeloggte
 * User JEDE Liste per ID aendern/archivieren/Mitglieder verwalten, wenn er
 * deren ID kennt - es gibt noch keine Pruefung "ist der User Owner/Member
 * dieser Liste". Fuer den privaten Nutzungsrahmen (Du + Familie) aktuell
 * vertretbar, sollte aber vor breiterer Nutzung ergaenzt werden.
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
    public ResponseEntity<ListResponse> create(@AuthenticationPrincipal Long userId,
                                                @Valid @RequestBody CreateListRequest request) {
        AppUser owner = userService.get(userId);
        ShoppingList list = listService.create(request.name(), request.einkaufsdatum(), owner);
        return ResponseEntity.created(URI.create("/api/lists/" + list.getId()))
                .body(ListResponse.from(list));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateListRequest request) {
        listService.update(id, request.name(), request.einkaufsdatum());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<ListResponse> getAccessibleLists(@AuthenticationPrincipal Long userId) {
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        listService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public MemberResponse addMember(@PathVariable Long id, @Valid @RequestBody AddMemberRequest request) {
        // getByEmail() nur fuer die freundliche Fehlermeldung bei unbekannter
        // E-Mail - weitergereicht wird nur die ID, siehe ListService.addMember.
        AppUser user = userService.getByEmail(request.email());
        ListMember member = listService.addMember(id, user.getId());
        return MemberResponse.from(member);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        listService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

}
