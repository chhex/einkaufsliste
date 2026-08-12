package ch.chris.einkaufsliste;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Minimaler Smoke-Test: laedt nur den Spring-Context, ohne DB-Verbindung.
 * Ab Schritt 2 (Docker-Datenbank) kommt ein Integrationstest gegen eine
 * echte Postgres-Instanz via Testcontainers dazu.
 */
@SpringBootTest
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
class EinkaufslisteApplicationTests {

    @Test
    void contextLoads() {
    }

}
