package ch.chris.einkaufsliste.web;

import ch.chris.einkaufsliste.domain.entity.AppUser;
import ch.chris.einkaufsliste.security.JwtService;
import ch.chris.einkaufsliste.service.UserService;
import ch.chris.einkaufsliste.web.dto.AuthResponse;
import ch.chris.einkaufsliste.web.dto.DevLoginRequest;
import ch.chris.einkaufsliste.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PROVISORISCH: simuliert das, was der Google-OAuth2-Callback tut
 * (UserService.findOrCreateByGoogleLogin mit bereits verifizierten Profildaten
 * aufrufen), UND stellt wie AuthController ein echtes JWT aus - damit curl-
 * Tests gegen die echte (nicht-permissive) SecurityConfig funktionieren,
 * ohne einen echten Google-Login durchzufuehren.
 * <p>
 * @Profile("!prod"): existiert als Bean NICHT im "prod"-Profil (aktiv via
 * SPRING_PROFILES_ACTIVE=prod in docker-compose.prod.yml) - in Produktion
 * ist dieser Endpoint damit komplett unerreichbar (404, nicht nur
 * abgesichert), unabhaengig vom permitAll-Eintrag in SecurityConfig. Lokal
 * (kein aktives Profil) und im "test"-Profil bleibt er verfuegbar.
 */
@RestController
@RequestMapping("/api/dev")
@Profile("!prod")
public class DevLoginController {

    private final UserService userService;
    private final JwtService jwtService;

    public DevLoginController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody DevLoginRequest request) {
        AppUser user = userService.findOrCreateByGoogleLogin(
                request.googleId(), request.email(), request.name());
        String jwt = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(jwt, UserResponse.from(user));
    }

}
