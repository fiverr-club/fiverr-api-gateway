package club.fiverr.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class JwtTokenValidator {

    private final SecretKey secretKey;

    public JwtTokenValidator(
            @Value("${security.jwt.secret:fiverr-club-super-secret-signing-key-must-be-at-least-256-bits-long-2026}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    public Optional<JwtClaimsPayload> validateAndExtract(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            String email = claims.get("email", String.class);
            List<String> roles = claims.get("roles", List.class);
            if (roles == null) {
                roles = Collections.emptyList();
            }
            return Optional.of(new JwtClaimsPayload(subject, email != null ? email : "", roles));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
