package ch.chris.einkaufsliste.service;

/**
 * Wird geworfen, wenn ein authentifizierter User eine Aktion versucht, fuer
 * die er keine Berechtigung hat (z.B. eine Liste aendern, deren Owner/
 * Mitglied er nicht ist). Bewusst getrennt von IllegalArgumentException
 * (400 "ungueltige Anfrage"/"nicht gefunden") - eine Autorisierungs-
 * Verweigerung ist semantisch etwas anderes (403 "verboten"), siehe
 * GlobalExceptionHandler.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
