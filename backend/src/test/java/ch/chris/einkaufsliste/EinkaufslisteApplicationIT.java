package ch.chris.einkaufsliste;

import ch.chris.einkaufsliste.domain.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke-Test: laedt den vollen Spring-Context inkl. echter Postgres via
 * Testcontainers (AbstractIntegrationTest) - seit es echte JPA-Repositories/
 * Services gibt (Schritt 3b/4), braucht der Context eine echte DB-
 * Infrastruktur, kein DB-loser Sonderfall mehr wie im urspruenglichen
 * Schritt-1-Grundgeruest.
 */
@SpringBootTest
class EinkaufslisteApplicationIT extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }

}
