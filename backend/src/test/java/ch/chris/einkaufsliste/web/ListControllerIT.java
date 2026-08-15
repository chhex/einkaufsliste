package ch.chris.einkaufsliste.web;

import ch.chris.einkaufsliste.domain.AbstractIntegrationTest;
import ch.chris.einkaufsliste.domain.entity.AppUser;
import ch.chris.einkaufsliste.domain.repository.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Test
    void createGefolgtVonGetLiefertDieErstellteListe() throws Exception {
        AppUser owner = appUserRepository.save(new AppUser("g-" + System.nanoTime(), "x@x.com", "X"));

        String createBody = objectMapper.writeValueAsString(new CreateListRequestJson("Migros", owner.getId()));

        String location = mockMvc.perform(post("/api/lists")
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
        String body = objectMapper.writeValueAsString(new CreateListRequestJson("", owner.getId()));

        mockMvc.perform(post("/api/lists")
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
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new CreateListRequestJson("Meine Liste", owner.getId()))));
        mockMvc.perform(post("/api/lists")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new CreateListRequestJson("Fremde Liste", andererUser.getId()))));

        mockMvc.perform(get("/api/lists").param("userId", owner.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Meine Liste"));
    }

    private record CreateListRequestJson(String name, Long ownerId) {
    }

}
