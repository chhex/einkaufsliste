package ch.chris.einkaufsliste.web;

import ch.chris.einkaufsliste.domain.AbstractIntegrationTest;
import ch.chris.einkaufsliste.domain.entity.AppUser;
import ch.chris.einkaufsliste.domain.repository.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TestSecurityConfig (test-Profil) laesst zwar alle Requests durch, aber
 * @AuthenticationPrincipal braucht trotzdem eine gesetzte Authentication in
 * der Request-MockMvc-Chain - simuliert hier per authentication(...)
 * Post-Processor, ohne echten Google-Login/JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ListControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    private static UsernamePasswordAuthenticationToken authFor(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    @Test
    void createGefolgtVonGetLiefertDieErstellteListe() throws Exception {
        AppUser owner = appUserRepository.save(new AppUser("g-" + System.nanoTime(), "x@x.com", "X"));

        String createBody = objectMapper.writeValueAsString(new CreateListRequestJson("Migros"));

        String location = mockMvc.perform(post("/api/lists")
                        .with(authentication(authFor(owner.getId())))
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Migros"))
                .andExpect(jsonPath("$.status").value("AKTIV"))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Migros"))
                .andExpect(jsonPath("$.ownerName").value("X"))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void createMitLeeremNamenLiefert400() throws Exception {
        AppUser owner = appUserRepository.save(new AppUser("g-" + System.nanoTime(), "y@y.com", "Y"));
        String body = objectMapper.writeValueAsString(new CreateListRequestJson(""));

        mockMvc.perform(post("/api/lists")
                        .with(authentication(authFor(owner.getId())))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getAccessibleListsLiefertNurListenDesUsers() throws Exception {
        AppUser owner = appUserRepository.save(new AppUser("g-" + System.nanoTime(), "z@z.com", "Z"));
        AppUser andererUser = appUserRepository.save(new AppUser("g-" + System.nanoTime(), "a@a.com", "A"));

        mockMvc.perform(post("/api/lists")
                .with(authentication(authFor(owner.getId())))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new CreateListRequestJson("Meine Liste"))));
        mockMvc.perform(post("/api/lists")
                .with(authentication(authFor(andererUser.getId())))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new CreateListRequestJson("Fremde Liste"))));

        mockMvc.perform(get("/api/lists").with(authentication(authFor(owner.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Meine Liste"));
    }

    private record CreateListRequestJson(String name) {
    }

}
