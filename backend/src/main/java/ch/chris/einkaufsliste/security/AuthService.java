package ch.chris.einkaufsliste.security;

import ch.chris.einkaufsliste.domain.entity.AppUser;
import ch.chris.einkaufsliste.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Orchestriert den Login-Fluss: Frontend macht den eigentlichen Google-Login
 * (Google Identity Services) und schickt uns nur das resultierende
 * ID-Token. Wir verifizieren dessen Signatur/Aussteller/Audience bei Google,
 * lesen die verifizierten Profildaten aus und provisionieren/finden den
 * User darueber (UserService, unveraendert seit Schritt 4). Anschliessend
 * stellen wir unser EIGENES JWT aus, das der Client fuer alle weiteren
 * API-Aufrufe nutzt.
 */
@Service
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public AuthService(UserService userService,
                        JwtService jwtService,
                        @Value("${app.google.client-id}") String googleClientId) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.googleIdTokenVerifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    /**
     * @return das eigene JWT fuer den (neuen oder bestehenden) User
     * @throws IllegalArgumentException wenn das Google-ID-Token ungueltig ist
     */
    public LoginResult loginWithGoogleIdToken(String googleIdTokenString) {
        GoogleIdToken idToken;
        try {
            idToken = googleIdTokenVerifier.verify(googleIdTokenString);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalArgumentException("Google-ID-Token konnte nicht verifiziert werden", e);
        }

        if (idToken == null) {
            throw new IllegalArgumentException("Ungueltiges Google-ID-Token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        AppUser user = userService.findOrCreateByGoogleLogin(googleId, email, name);
        String jwt = jwtService.generateToken(user.getId(), user.getEmail());

        return new LoginResult(jwt, user);
    }

    public record LoginResult(String jwt, AppUser user) {
    }

}
