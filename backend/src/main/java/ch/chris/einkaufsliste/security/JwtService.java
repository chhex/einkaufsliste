package ch.chris.einkaufsliste.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

/**
 * Stellt eigene, selbst signierte JWTs aus (nach erfolgreicher Google-Login-
 * Verifikation, siehe AuthService) und validiert sie bei nachfolgenden
 * API-Aufrufen (siehe JwtAuthenticationFilter). Bewusst stateless - kein
 * Server-seitiger Session-Speicher noetig, robust auch bei Render-Cold-Starts.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String generateToken(Long userId, String email) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expiration.toMillis());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(key)
                .compact();
    }

    /**
     * Liefert die User-ID aus einem gueltigen, nicht abgelaufenen Token -
     * oder Optional.empty() bei ungueltiger Signatur/abgelaufenem Token,
     * statt eine Exception zu werfen (der Filter behandelt das als
     * "nicht authentifiziert", nicht als Server-Fehler).
     */
    public Optional<Long> validateAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

}
