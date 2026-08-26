package ch.chris.einkaufsliste.config;

import ch.chris.einkaufsliste.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Echte Security-Konfiguration: JWT-Bearer-Auth (siehe JwtAuthenticationFilter),
 * stateless (kein Server-Session-Speicher noetig, robust bei Render-Cold-Starts).
 * <p>
 * Aktiv in allen Profilen AUSSER "test" - Tests nutzen stattdessen
 * TestSecurityConfig (permissiv), damit IT-Tests keine echten Google-Logins
 * durchfuehren muessen. Siehe AbstractIntegrationTest (@ActiveProfiles("test")).
 */
@Configuration
@Profile("!test")
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CORS-Preflight-Requests (OPTIONS) tragen NIE einen
                        // Authorization-Header (Browser-Vorgabe) - muessen
                        // deshalb immer durchgelassen werden, sonst schlaegt
                        // schon die Preflight-Anfrage mit 403 fehl, bevor der
                        // eigentliche Request (z.B. POST) ueberhaupt rausgeht.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/ping",
                                "/api/auth/**",
                                // TODO: /api/dev/** ist eine Test-Hintertuer (siehe
                                // DevLoginController) - vor "richtigem" Go-Live
                                // entfernen oder hinter ein eigenes Profil stellen.
                                "/api/dev/**",
                                "/actuator/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // TODO: Vercel-Produktions-URL des Svelte-Frontends ergaenzen, sobald
        // Schritt 6/7 (Client-Deployment) steht.
        // localhost:8000 ist fuer die Standalone-Google-Login-Testseite
        // (scripts/auth-test/, per "python3 -m http.server" served).
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:8000",
                "https://*.vercel.app"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
