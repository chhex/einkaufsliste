package ch.chris.einkaufsliste.web;

import ch.chris.einkaufsliste.domain.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Nutzt bewusst NICHT das "test"-Profil (das wuerde TestSecurityConfig,
 * permissiv, aktivieren) - inheritProfiles = false ist hier entscheidend:
 * ohne das wuerde @ActiveProfiles({}) das "test"-Profil der Basisklasse
 * (AbstractIntegrationTest) trotzdem erben/mergen (Spring's Default fuer
 * @ActiveProfiles ist additiv), TestSecurityConfig bliebe also weiterhin
 * aktiv - genau das ist beim ersten Versuch hier auch passiert (Test
 * "bestand" wegen der zu permissiven Config, statt echt zu pruefen).
 * <p>
 * Grund fuer den ganzen Test: genau diese Kombination (echte SecurityConfig
 * + kein passender Handler) hat uns live auf comprarli.com ueberrascht -
 * Spring Boots interner Forward auf "/error" bei einem 404 wurde von der
 * Security-Chain selbst blockiert (da "/error" nicht in der permitAll-Liste
 * stand), wodurch JEDE 404-Situation faelschlich als 403 ankam. Mit
 * TestSecurityConfig (permissiv) waere dieser Bug in normalen IT-Tests nie
 * aufgefallen.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(profiles = {}, inheritProfiles = false)
class SecurityConfigIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void nichtGefundenerEndpointLiefert404NichtFaelschlich403() throws Exception {
        mockMvc.perform(get("/api/dieser-endpoint-existiert-nicht"))
                .andExpect(status().isNotFound());
    }

    @Test
    void pingBleibtOhneAuthErreichbar() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk());
    }

    @Test
    void geschuetzterEndpointOhneTokenLiefert403() throws Exception {
        // Zur Abgrenzung: ein ECHT geschuetzter, existierender Endpoint soll
        // weiterhin 403 liefern (nicht durch den /error-Fix versehentlich
        // ebenfalls permitAll geworden sein).
        mockMvc.perform(get("/api/lists"))
                .andExpect(status().isForbidden());
    }

}
