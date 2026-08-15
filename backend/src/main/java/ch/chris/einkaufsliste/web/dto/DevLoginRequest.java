package ch.chris.einkaufsliste.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DevLoginRequest(
        @NotBlank String googleId,
        @NotBlank @Email String email,
        @NotBlank String name
) {
}
