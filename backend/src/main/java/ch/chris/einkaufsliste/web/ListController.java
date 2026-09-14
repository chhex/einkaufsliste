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
 * "userId" kommt hier NIE vom Client (Query/Body) fuer die Frage "wer
 * bin ich" - immer ueber @AuthenticationPrincipal aus dem verifizierten JWT
 * (siehe JwtAuthenticationFilter). Waere es weiterhin ein Client-Parameter,
 * koennte sich ein eingeloggter User als beliebiger anderer User ausgeben.
 * <p>
 * Autorisierung (wer darf WAS mit einer bestimmten Liste tun): jeder
 * Endpoint mit einer Listen-ID im Pfad ruft zuerst listService.assertAccess
 * (Owner ODER Mitglied - fuer kollaborative Aktionen: lesen, Items pflegen,
 * Name/Datum aendern, archivieren/reaktivieren) oder listService.assertOwner
 * (nur der Owner - fuer destruktive/verwaltende Aktionen: Liste loeschen,
 * Mitglieder hinzufuegen/entfernen) auf, bevor irgendwas passiert. Eine
 * fehlende Berechtigung wirft ForbiddenException -> 403 (siehe
 * GlobalExceptionHandler).
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
        // Keine Autorisierungspruefung noetig: eine neue Liste anzulegen
        // erfordert keine bestehende Berechtigung, der Ersteller wird
        // automatisch Owner.
        AppUser owner = userService.get(userId);
        ShoppingList list = listService.create(request.name(), request.einkaufsdatum(), owner);
        return ResponseEntity.created(URI.create("/api/lists/" + list.getId()))
                .body(ListResponse.from(list));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long id,
                                        @Valid @RequestBody UpdateListRequest request) {
        listService.assertAccess(id, userId);
        listService.update(id, request.name(), request.einkaufsdatum());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<ListResponse> getAccessibleLists(@AuthenticationPrincipal Long userId) {
        // Keine separate Pruefung noetig: die Query selbst filtert bereits
        // auf Listen, bei denen der User Owner/Mitglied ist.
        return listService.getAccessibleByUser(userId).stream()
                .map(ListResponse::summary)
                .toList();
    }

    @GetMapping("/{id}")
    public ListResponse get(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        listService.assertAccess(id, userId);
        return ListResponse.from(listService.get(id));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        listService.assertAccess(id, userId);
        listService.archive(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        listService.assertAccess(id, userId);
        listService.reactivate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        // Bewusst assertOwner statt assertAccess: eine Liste komplett zu
        // loeschen betrifft alle Mitglieder, das soll nicht jedes einzelne
        // Mitglied im Alleingang koennen.
        listService.assertOwner(id, userId);
        listService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public MemberResponse addMember(@AuthenticationPrincipal Long userId,
                                     @PathVariable Long id,
                                     @Valid @RequestBody AddMemberRequest request) {
        // Mitglieder-Verwaltung bewusst Owner-only (wie delete oben).
        listService.assertOwner(id, userId);
        // getByEmail() nur fuer die freundliche Fehlermeldung bei unbekannter
        // E-Mail - weitergereicht wird nur die ID, siehe ListService.addMember.
        AppUser user = userService.getByEmail(request.email());
        ListMember member = listService.addMember(id, user.getId());
        return MemberResponse.from(member);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@AuthenticationPrincipal Long requesterId,
                                              @PathVariable Long id,
                                              @PathVariable Long userId) {
        // Ausnahme von "Owner-only": ein Mitglied darf sich selbst
        // entfernen (die Liste verlassen), ohne Owner zu sein.
        if (!requesterId.equals(userId)) {
            listService.assertOwner(id, requesterId);
        }
        listService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

}
