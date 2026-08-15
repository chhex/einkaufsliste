package ch.chris.einkaufsliste.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ItemRequest(
        @NotBlank String bezeichnung,
        @NotNull @Positive BigDecimal menge,
        @NotBlank String einheit,
        String kategorie
) {
}
