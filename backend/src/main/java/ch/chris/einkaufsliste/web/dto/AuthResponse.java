package ch.chris.einkaufsliste.web.dto;

public record AuthResponse(String token, UserResponse user) {
}
