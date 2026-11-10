package ch.chris.einkaufsliste.config;

import ch.chris.einkaufsliste.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.Arrays;
import java.util.List;

/**
 * Echte Security-Konfiguration: JWT-Bearer-Auth (siehe JwtAuthenticationFilter),
 * stateless (kein Server-Session-Speicher noetig).
 * <p>
 * Aktiv in allen Profilen AUSSER "test" - Tests nutzen stattdessen
 * TestSecurityConfig (permissiv), damit IT-Tests keine echten Google-Logins
 * durchfuehren muessen. Siehe AbstractIntegrationTest (@ActiveProfiles("test")).
 */
@Configuration
@Profile("!test")
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Erlaubte CORS-Origins, kommagetrennt (app.cors.allowed-origins, per
     * Umgebungsvariable CORS_ALLOWED_ORIGINS ueberschreibbar - siehe
     * application.yml). Noetig z.B. fuer mehrere lokale Frontend-Ports oder
     * den Test vom iPhone/iPad ueber die LAN-Adresse des Macs.
     */
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

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
                                // Test-Hintertuer (siehe DevLoginController) - existiert als
                                // Bean im "prod"-Profil gar nicht mehr (@Profile("!prod")),
                                // dieser permitAll-Eintrag ist dort also wirkungslos/harmlos.
                                "/api/dev/**",
                                "/actuator/**",
                                // Spring Boots interne Fehlerbehandlung: wenn kein Handler
                                // gefunden wird (z.B. genau der Fall oben, DevLoginController
                                // fehlt in Produktion), leitet der Container intern auf /error
                                // weiter - das ist aus Security-Sicht ein NEUER Request und
                                // faellt ohne diesen Eintrag unter anyRequest().authenticated(),
                                // wird selbst mit 403 abgelehnt. Ergebnis ohne diesen Eintrag:
                                // JEDE 404-Situation wird faelschlich zu einem 403.
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Defaults (application.yml): localhost:5173 (Vite), localhost:8000
        // (Standalone-Google-Login-Testseite, scripts/auth-test/) und die
        // Produktions-Domains.
        config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
