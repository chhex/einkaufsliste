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
 * permissiv, aktivieren) - ueberschreibt AbstractIntegrationTest's
 * @ActiveProfiles("test") mit einer leeren Liste, damit stattdessen die
 * ECHTE SecurityConfig greift (@Profile("!test")).
 * <p>
 * Grund: genau diese Kombination (echte SecurityConfig + kein passender
 * Handler) hat uns live auf comprarli.com ueberrascht - Spring Boots
 * interner Forward auf "/error" bei einem 404 wurde von der Security-Chain
 * selbst blockiert (da "/error" nicht in der permitAll-Liste stand),
 * wodurch JEDE 404-Situation faelschlich als 403 ankam. Mit TestSecurityConfig
 * (permissiv) waere dieser Bug in den normalen IT-Tests nie aufgefallen.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({})
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
