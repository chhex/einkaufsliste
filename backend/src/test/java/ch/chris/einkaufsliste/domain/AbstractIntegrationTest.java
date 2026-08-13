package ch.chris.einkaufsliste.domain;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Basisklasse: startet eine echte Postgres-Instanz per Testcontainers und
 * verdrahtet Spring Boot (Datasource + Flyway) darauf. So laufen Repository-
 * Tests gegen dieselbe Engine wie in Produktion (kein H2-Ersatz), inkl.
 * echter Ausfuehrung von V1-V3-Migrationen.
 */
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("einkaufsliste_test")
                    .withUsername("test_admin")
                    .withPassword("test_admin");

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
