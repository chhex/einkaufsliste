package ch.chris.einkaufsliste.web;

import ch.chris.einkaufsliste.domain.AbstractIntegrationTest;
import ch.chris.einkaufsliste.domain.entity.AppUser;
import ch.chris.einkaufsliste.domain.entity.ShoppingList;
import ch.chris.einkaufsliste.domain.repository.AppUserRepository;
import ch.chris.einkaufsliste.service.ListService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ListService listService;

    private ShoppingList list() {
        AppUser owner = appUserRepository.save(new AppUser("g-" + System.nanoTime(), "x@x.com", "X"));
        return listService.create("Testliste", null, owner);
    }

    @Test
    void addItemUndAbrufenUeberDieListe() throws Exception {
        ShoppingList list = list();
        String body = objectMapper.writeValueAsString(new ItemRequestJson("Tomaten", new BigDecimal("500"), "g", "Gemüse"));

        mockMvc.perform(post("/api/lists/" + list.getId() + "/items")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bezeichnung").value("Tomaten"))
                .andExpect(jsonPath("$.abgehakt").value(false));

        mockMvc.perform(get("/api/lists/" + list.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].bezeichnung").value("Tomaten"));
    }

    @Test
    void toggleAbgehaktArchiviertListeAutomatischWennAlleAbgehakt() throws Exception {
        ShoppingList list = list();
        String bodyA = objectMapper.writeValueAsString(new ItemRequestJson("Kaffee", BigDecimal.ONE, "Pkg", null));

        String locationA = mockMvc.perform(post("/api/lists/" + list.getId() + "/items")
                        .contentType("application/json")
                        .content(bodyA))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(patch(locationA + "/abgehakt")
                        .contentType("application/json")
                        .content("{\"abgehakt\": true}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/lists/" + list.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVIERT"));
    }

    @Test
    void deleteEntferntItem() throws Exception {
        ShoppingList list = list();
        String body = objectMapper.writeValueAsString(new ItemRequestJson("Brot", BigDecimal.ONE, "Stk", null));

        String location = mockMvc.perform(post("/api/lists/" + list.getId() + "/items")
                        .contentType("application/json")
                        .content(body))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(delete(location))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/lists/" + list.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    private record ItemRequestJson(String bezeichnung, BigDecimal menge, String einheit, String kategorie) {
    }

}
