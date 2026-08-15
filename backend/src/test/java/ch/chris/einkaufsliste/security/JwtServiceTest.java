package ch.chris.einkaufsliste.security;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reiner Unit-Test: kein Spring-Context, kein Docker.
 */
class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-mindestens-32-bytes-lang-genug", 60);

    @Test
    void generiertTokenUndValidiertEsErfolgreich() {
        String token = jwtService.generateToken(42L, "chris@example.com");

        Optional<Long> userId = jwtService.validateAndGetUserId(token);

        assertThat(userId).contains(42L);
    }

    @Test
    void lehntManipuliertesTokenAb() {
        String token = jwtService.generateToken(1L, "x@x.com");
        String manipuliert = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.validateAndGetUserId(manipuliert)).isEmpty();
    }

    @Test
    void lehntTokenVonAnderemSecretAb() {
        JwtService andererService = new JwtService("ein-komplett-anderes-secret-32-bytes-lang", 60);
        String token = andererService.generateToken(1L, "x@x.com");

        assertThat(jwtService.validateAndGetUserId(token)).isEmpty();
    }

    @Test
    void lehntBereitsAbgelaufenesTokenAb() {
        // negative Gueltigkeitsdauer -> Token ist im Moment der Erstellung
        // bereits abgelaufen (expiresAt liegt in der Vergangenheit)
        JwtService abgelaufenService = new JwtService("noch-ein-anderes-secret-32-bytes-lang-x", -1);
        String token = abgelaufenService.generateToken(1L, "x@x.com");

        assertThat(abgelaufenService.validateAndGetUserId(token)).isEmpty();
    }

}
