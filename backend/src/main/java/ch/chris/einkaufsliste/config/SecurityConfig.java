package ch.chris.einkaufsliste.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * PROVISORISCH: spring-boot-starter-oauth2-client aktiviert automatisch
 * Spring Security, das ohne explizite Konfiguration alles per Basic-Auth
 * sperrt (401). Bis die echte Google-OAuth2-Anbindung fuer Multiuser/Sharing
 * gebaut ist (spaeterer Schritt), geben wir hier alles frei, damit die
 * REST-Endpunkte waehrend Schritt 2-5 ungehindert getestet werden koennen.
 *
 * TODO: ersetzen durch echte OAuth2-Login-Konfiguration + Endpoint-Schutz.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

}
