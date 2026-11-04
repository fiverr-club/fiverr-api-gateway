package club.fiverr.gateway.security;

import java.util.List;

public record JwtClaimsPayload(
        String userId,
        String email,
        List<String> roles
) {
}
