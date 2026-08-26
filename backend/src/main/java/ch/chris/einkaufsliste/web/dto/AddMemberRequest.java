package ch.chris.einkaufsliste.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Mitglieder werden per E-Mail gesucht (nicht per userId, die man als
 * Aufrufer gar nicht kennt) - siehe UserService.getByEmail.
 */
public record AddMemberRequest(@NotBlank @Email String email) {
}
