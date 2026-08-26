package ch.chris.einkaufsliste.web.dto;

import java.time.LocalDate;

/**
 * Beide Felder optional - null bedeutet "unveraendert lassen" (siehe
 * ListService.update).
 */
public record UpdateListRequest(String name, LocalDate einkaufsdatum) {
}
