package ch.chris.einkaufsliste.domain;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Basisklasse: startet eine echte Postgres-Instanz per Testcontainers und
 * verdrahtet Spring Boot (Datasource + Flyway) darauf. So laufen Repository-/
 * Service-Tests gegen dieselbe Engine wie in Produktion (kein H2-Ersatz),
 * inkl. echter Ausfuehrung von V1-V3-Migrationen.
 * <p>
 * SINGLETON-CONTAINER-PATTERN: der Container wird in einem statischen
 * Initializer-Block EINMALIG gestartet und NIE explizit gestoppt. Das ist
 * bewusst so - dieses Feld ist static in der gemeinsamen Basisklasse und wird
 * daher von ALLEN *IT-Testklassen geteilt (nicht pro Klasse neu angelegt).
 * Mit der frueheren @Container-Annotation stoppte JUnit den Container nach
 * jeder Testklasse automatisch, was beim Wechsel zur naechsten *IT-Klasse zu
 * toten Connections fuehrte ("Failed to validate connection ... connection
 * has been closed"). Testcontainers' eigener Ryuk-Reaper raeumt den
 * Container automatisch beim JVM-Ende auf, ein manuelles stop() ist nicht
 * noetig.
 */
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
                .withDatabaseName("einkaufsliste_test")
                .withUsername("test_admin")
                .withPassword("test_admin");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Im Test gibt es keine separate Admin-/App-User-Trennung -
        // Flyway und die App-Datasource laufen beide ueber den Testcontainer-User.
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

}
