package ch.chris.einkaufsliste.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateListRequest(
        @NotBlank String name,
        // TODO: sobald Google-OAuth2-Login verdrahtet ist (SecurityConfig
        // ist aktuell noch provisorisch offen), kommt der Owner aus dem
        // authentifizierten Principal statt explizit im Request-Body.
        @NotNull Long ownerId
) {
}
