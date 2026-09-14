package ch.chris.einkaufsliste.web;

import ch.chris.einkaufsliste.domain.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nutzt bewusst NICHT das "test"-Profil (das wuerde TestSecurityConfig,
 * permissiv, aktivieren) - inheritProfiles = false ist hier entscheidend:
 * ohne das wuerde @ActiveProfiles({}) das "test"-Profil der Basisklasse
 * (AbstractIntegrationTest) trotzdem erben/mergen (Spring's Default fuer
 * @ActiveProfiles ist additiv), TestSecurityConfig bliebe also weiterhin
 * aktiv statt der echten SecurityConfig.
 * <p>
 * webEnvironment = RANDOM_PORT + TestRestTemplate statt MockMvc: der
 * Live-Bug, den dieser Test absichert (interner "/error"-Forward wird von
 * der Security-Chain selbst blockiert), haengt an Spring Boots
 * ErrorPageFilter - der ist an einen ECHTEN eingebetteten Servlet-Container
 * gekoppelt und wird in MockMvcs simulierter Umgebung (ohne echten Tomcat)
 * nicht zuverlaessig repliziert. Nur mit einem wirklich laufenden Server
 * (RANDOM_PORT) und einem echten HTTP-Client (TestRestTemplate) laesst sich
 * dieses Verhalten wahrheitsgetreu testen.
 * <p>
 * Grund fuer den Test: genau diese Kombination (echte SecurityConfig + kein
 * passender Handler) hat uns live auf comprarli.com ueberrascht - jede
 * 404-Situation kam faelschlich als 403 an, bevor "/error" zur
 * permitAll-Liste ergaenzt wurde.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = {}, inheritProfiles = false)
class SecurityConfigIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void nichtGefundenerEndpointInnerhalbEinerPermitAllRouteLiefert404NichtFaelschlich403() {
        // Wichtig: Zielt bewusst auf einen Unterpfad INNERHALB einer
        // bereits permitAll'ten Route (/api/dev/**), fuer den es aber
        // keinen Handler gibt - reproduziert damit exakt das Live-Szenario
        // (Security laesst die urspruengliche Anfrage durch, ERST DANACH
        // scheitert die Handler-Suche und loest den echten /error-Forward
        // des Servlet-Containers aus). Ein komplett unbekannter Pfad
        // ausserhalb jeder permitAll-Regel wuerde stattdessen schon bei der
        // urspruenglichen Anfrage selbst mit 403 blockiert werden - das
        // waere ein anderer Test (fehlende Auth), nicht der /error-Bug.
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/dev/dieser-unterpfad-existiert-nicht", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void pingBleibtOhneAuthErreichbar() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/ping", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void geschuetzterEndpointOhneTokenLiefert403() {
        // Zur Abgrenzung: ein ECHT geschuetzter, existierender Endpoint soll
        // weiterhin 403 liefern (nicht durch den /error-Fix versehentlich
        // ebenfalls permitAll geworden sein).
        ResponseEntity<String> response = restTemplate.getForEntity("/api/lists", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

}
