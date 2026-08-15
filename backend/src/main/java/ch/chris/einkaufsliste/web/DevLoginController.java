package ch.chris.einkaufsliste.web;

import ch.chris.einkaufsliste.domain.entity.AppUser;
import ch.chris.einkaufsliste.security.JwtService;
import ch.chris.einkaufsliste.service.UserService;
import ch.chris.einkaufsliste.web.dto.AuthResponse;
import ch.chris.einkaufsliste.web.dto.DevLoginRequest;
import ch.chris.einkaufsliste.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PROVISORISCH: simuliert das, was spaeter der Google-OAuth2-Callback tut
 * (UserService.findOrCreateByGoogleLogin mit bereits verifizierten Profildaten
 * aufrufen), UND stellt wie AuthController ein echtes JWT aus - damit curl-
 * Tests gegen die echte (nicht-permissive) SecurityConfig funktionieren,
 * ohne einen echten Google-Login durchzufuehren.
 * <p>
 * MUSS entfernt oder hinter ein "nur lokal aktiv"-Profil gestellt werden,
 * sobald echtes Google-OAuth2 im Frontend steht - sonst koennte sich jeder
 * als jeder beliebige User ausgeben (siehe permitAll-Eintrag in SecurityConfig).
 */
@RestController
@RequestMapping("/api/dev")
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
