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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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

    private static UsernamePasswordAuthenticationToken authFor(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private AppUser owner;

    private ShoppingList list() {
        owner = appUserRepository.save(new AppUser("g-" + System.nanoTime(), "x@x.com", "X"));
        return listService.create("Testliste", null, owner);
    }

    @Test
    void addItemUndAbrufenUeberDieListe() throws Exception {
        ShoppingList list = list();
        String body = objectMapper.writeValueAsString(new ItemRequestJson("Tomaten", new BigDecimal("500"), "g", "Gemüse"));

        mockMvc.perform(post("/api/lists/" + list.getId() + "/items")
                        .with(authentication(authFor(owner.getId())))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bezeichnung").value("Tomaten"))
                .andExpect(jsonPath("$.abgehakt").value(false));

        mockMvc.perform(get("/api/lists/" + list.getId()).with(authentication(authFor(owner.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].bezeichnung").value("Tomaten"));
    }

    @Test
    void toggleAbgehaktArchiviertListeAutomatischWennAlleAbgehakt() throws Exception {
        ShoppingList list = list();
        String bodyA = objectMapper.writeValueAsString(new ItemRequestJson("Kaffee", BigDecimal.ONE, "Pkg", null));

        String locationA = mockMvc.perform(post("/api/lists/" + list.getId() + "/items")
                        .with(authentication(authFor(owner.getId())))
                        .contentType("application/json")
                        .content(bodyA))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(patch(locationA + "/abgehakt")
                        .with(authentication(authFor(owner.getId())))
                        .contentType("application/json")
                        .content("{\"abgehakt\": true}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/lists/" + list.getId()).with(authentication(authFor(owner.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVIERT"));
    }

    @Test
    void deleteEntferntItem() throws Exception {
        ShoppingList list = list();
        String body = objectMapper.writeValueAsString(new ItemRequestJson("Brot", BigDecimal.ONE, "Stk", null));

        String location = mockMvc.perform(post("/api/lists/" + list.getId() + "/items")
                        .with(authentication(authFor(owner.getId())))
                        .contentType("application/json")
                        .content(body))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(delete(location).with(authentication(authFor(owner.getId()))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/lists/" + list.getId()).with(authentication(authFor(owner.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void fremderUserBekommt403BeimHinzufuegenEinesItems() throws Exception {
        ShoppingList list = list(); // setzt owner
        AppUser fremder = appUserRepository.save(new AppUser("g-fremd-" + System.nanoTime(), "fremd@x.com", "Fremd"));
        String body = objectMapper.writeValueAsString(new ItemRequestJson("Heimlich", BigDecimal.ONE, "Stk", null));

        mockMvc.perform(post("/api/lists/" + list.getId() + "/items")
                        .with(authentication(authFor(fremder.getId())))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void fremderUserBekommt403BeimAbhakenEinesItems() throws Exception {
        ShoppingList list = list(); // setzt owner
        AppUser fremder = appUserRepository.save(new AppUser("g-fremd2-" + System.nanoTime(), "fremd2@x.com", "Fremd2"));
        String body = objectMapper.writeValueAsString(new ItemRequestJson("Kaese", BigDecimal.ONE, "Stk", null));

        String location = mockMvc.perform(post("/api/lists/" + list.getId() + "/items")
                        .with(authentication(authFor(owner.getId())))
                        .contentType("application/json")
                        .content(body))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(patch(location + "/abgehakt")
                        .with(authentication(authFor(fremder.getId())))
                        .contentType("application/json")
                        .content("{\"abgehakt\": true}"))
                .andExpect(status().isForbidden());
    }

    private record ItemRequestJson(String bezeichnung, BigDecimal menge, String einheit, String kategorie) {
    }

}
