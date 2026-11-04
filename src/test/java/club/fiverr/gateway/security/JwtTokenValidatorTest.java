package club.fiverr.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenValidatorTest {

    private static final String TEST_SECRET = "fiverr-club-super-secret-signing-key-must-be-at-least-256-bits-long-2026";
    private JwtTokenValidator validator;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        validator = new JwtTokenValidator(TEST_SECRET);
        secretKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldValidateValidJwtAndExtractClaims() {
        String userId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(userId)
                .claim("email", "freelancer@fiverr.club")
                .claim("roles", List.of("BUYER", "SELLER"))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(secretKey)
                .compact();

        Optional<JwtClaimsPayload> result = validator.validateAndExtract(token);

        assertTrue(result.isPresent());
        assertEquals(userId, result.get().userId());
        assertEquals("freelancer@fiverr.club", result.get().email());
        assertEquals(List.of("BUYER", "SELLER"), result.get().roles());
    }

    @Test
    void shouldRejectTamperedOrMalformedToken() {
        Optional<JwtClaimsPayload> result = validator.validateAndExtract("invalid.jwt.token");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectExpiredJwtToken() {
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "expired@fiverr.club")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(secretKey)
                .compact();

        Optional<JwtClaimsPayload> result = validator.validateAndExtract(token);
        assertTrue(result.isEmpty());
    }
}
