package ch.chris.einkaufsliste.service;

import ch.chris.einkaufsliste.domain.AbstractIntegrationTest;
import ch.chris.einkaufsliste.domain.entity.AppUser;
import ch.chris.einkaufsliste.domain.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceIT extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void erstelltNeuenUserBeimErstenLogin() {
        AppUser user = userService.findOrCreateByGoogleLogin("google-abc", "chris@example.com", "Chris");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("chris@example.com");
        assertThat(appUserRepository.findByGoogleId("google-abc")).isPresent();
    }

    @Test
    void zweiterLoginMitGleicherGoogleIdErzeugtKeinenDuplikatUser() {
        AppUser erster = userService.findOrCreateByGoogleLogin("google-xyz", "partner@example.com", "Partner");
        AppUser zweiter = userService.findOrCreateByGoogleLogin("google-xyz", "partner@example.com", "Partner");

        assertThat(zweiter.getId()).isEqualTo(erster.getId());
        assertThat(appUserRepository.findByGoogleId("google-xyz")).isPresent();
    }

    @Test
    void deleteEntferntDenUser() {
        AppUser user = userService.findOrCreateByGoogleLogin("google-del", "del@example.com", "ZuLoeschen");

        userService.delete(user.getId());

        assertThat(appUserRepository.findById(user.getId())).isEmpty();
    }

    @Test
    void getByEmailFindetBestehendenUser() {
        userService.findOrCreateByGoogleLogin("google-mail", "gesucht@example.com", "Gesucht");

        AppUser gefunden = userService.getByEmail("gesucht@example.com");

        assertThat(gefunden.getName()).isEqualTo("Gesucht");
    }

    @Test
    void getByEmailWirftFehlerWennNochNieEingeloggt() {
        assertThatThrownBy(() -> userService.getByEmail("nie-eingeloggt@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
