package ch.chris.einkaufsliste.service;

import ch.chris.einkaufsliste.domain.entity.AppUser;
import ch.chris.einkaufsliste.domain.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisionierung von Usern beim Google-Login (find-or-create). Der
 * eigentliche OAuth2-Handshake wird erst spaeter verdrahtet (aktuell
 * SecurityConfig noch provisorisch offen) - dieser Service ist bewusst
 * unabhaengig davon: er nimmt bereits verifizierte Google-Profildaten
 * entgegen und kuemmert sich nur um die Persistenz.
 */
@Service
public class UserService {

    private final AppUserRepository appUserRepository;

    public UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * Liefert den bestehenden User zu dieser googleId, oder legt beim
     * ersten Login einen neuen an. Idempotent: mehrfacher Aufruf mit
     * derselben googleId erzeugt keinen Duplikat-User.
     */
    @Transactional
    public AppUser findOrCreateByGoogleLogin(String googleId, String email, String name) {
        return appUserRepository.findByGoogleId(googleId)
                .orElseGet(() -> appUserRepository.save(new AppUser(googleId, email, name)));
    }

    /**
     * Loescht einen User hart. ACHTUNG: cascaded per DB-FK (ON DELETE CASCADE)
     * auf alle Listen, deren Owner dieser User ist, inkl. deren Items -
     * bewusst so gewaehlt, siehe V2__create_domain_schema.sql. Fuer Listen
     * sehen wir aktuell KEIN hartes Loeschen ueber die App vor (nur
     * Archivierung/Reaktivierung) - Nutzer-Loeschung ist ein Sonderfall.
     */
    @Transactional
    public void delete(Long userId) {
        appUserRepository.deleteById(userId);
    }

}
