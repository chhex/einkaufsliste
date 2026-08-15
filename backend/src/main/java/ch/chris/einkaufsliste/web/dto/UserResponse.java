package ch.chris.einkaufsliste.web.dto;

import ch.chris.einkaufsliste.domain.entity.AppUser;

public record UserResponse(Long id, String name, String email) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
