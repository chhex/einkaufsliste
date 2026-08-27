package ch.chris.einkaufsliste.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Nur bezeichnung ist Pflicht (Anforderung: reibungsloses Hinzufuegen,
 * Details spaeter nachtragbar) - menge/einheit/kategorie alle optional.
 */
public record ItemRequest(
        @NotBlank String bezeichnung,
        @Positive BigDecimal menge,
        String einheit,
        String kategorie
) {
}
