package ch.chris.einkaufsliste.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Nur im "test"-Profil aktiv (siehe AbstractIntegrationTest,
 * @ActiveProfiles("test")): gibt alles frei, damit IT-Tests/MockMvc-Tests
 * keine echten Google-Logins durchfuehren muessen, um API-Endpunkte zu
 * erreichen. Die "echte" SecurityConfig (JWT-Bearer-Auth) ist dafuer in
 * diesem Profil deaktiviert (@Profile("!test")).
 */
@Configuration
@Profile("test")
public class TestSecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

}
