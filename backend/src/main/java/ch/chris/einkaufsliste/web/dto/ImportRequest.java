package ch.chris.einkaufsliste.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ImportRequest(@NotBlank String rawText) {
}
