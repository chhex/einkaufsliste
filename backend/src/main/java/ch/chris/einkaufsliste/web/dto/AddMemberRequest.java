package ch.chris.einkaufsliste.web.dto;

import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(@NotNull Long userId) {
}
