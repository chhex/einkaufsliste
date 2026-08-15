package ch.chris.einkaufsliste.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateListRequest(
        @NotBlank String name
        // ownerId kommt NICHT mehr vom Client (Sicherheitsluecke: sonst
        // koennte sich ein eingeloggter User als beliebiger anderer User
        // ausgeben) - siehe ListController: der Owner ist immer der
        // authentifizierte User aus dem JWT.
) {
}
