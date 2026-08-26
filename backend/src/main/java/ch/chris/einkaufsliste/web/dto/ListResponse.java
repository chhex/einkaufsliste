package ch.chris.einkaufsliste.web.dto;

import ch.chris.einkaufsliste.domain.entity.ShoppingList;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ListResponse(
        Long id,
        String name,
        LocalDate einkaufsdatum,
        Long ownerId,
        String ownerName,
        String status,
        String sortierung,
        OffsetDateTime archivedAt,
        List<ItemResponse> items,
        List<MemberResponse> members
) {
    public static ListResponse from(ShoppingList list) {
        return new ListResponse(
                list.getId(),
                list.getName(),
                list.getEinkaufsdatum(),
                list.getOwner().getId(),
                list.getOwner().getName(),
                list.getStatus().name(),
                list.getSortierung().name(),
                list.getArchivedAt(),
                list.getItems().stream().map(ItemResponse::from).toList(),
                list.getMembers().stream().map(MemberResponse::from).toList()
        );
    }

    /**
     * Schlanke Variante ohne Items/Members - fuer Listenuebersichten
     * (GET /api/lists), wo die Details noch nicht gebraucht werden.
     */
    public static ListResponse summary(ShoppingList list) {
        return new ListResponse(
                list.getId(),
                list.getName(),
                list.getEinkaufsdatum(),
                list.getOwner().getId(),
                list.getOwner().getName(),
                list.getStatus().name(),
                list.getSortierung().name(),
                list.getArchivedAt(),
                List.of(),
                List.of()
        );
    }
}
