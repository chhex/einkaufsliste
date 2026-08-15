package ch.chris.einkaufsliste.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(@NotBlank String name) {
}
