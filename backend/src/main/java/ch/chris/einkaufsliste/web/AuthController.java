package ch.chris.einkaufsliste.web;

import ch.chris.einkaufsliste.security.AuthService;
import ch.chris.einkaufsliste.web.dto.AuthResponse;
import ch.chris.einkaufsliste.web.dto.GoogleAuthRequest;
import ch.chris.einkaufsliste.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Frontend macht den eigentlichen Google-Login (Google Identity
     * Services), schickt hierher nur das resultierende ID-Token. Antwort
     * enthaelt unser eigenes JWT fuer alle weiteren API-Aufrufe
     * ("Authorization: Bearer <token>").
     */
    @PostMapping("/google")
    public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        AuthService.LoginResult result = authService.loginWithGoogleIdToken(request.idToken());
        return new AuthResponse(result.jwt(), UserResponse.from(result.user()));
    }

}
