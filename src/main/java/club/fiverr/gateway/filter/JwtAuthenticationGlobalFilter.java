package club.fiverr.gateway.filter;

import club.fiverr.gateway.security.JwtClaimsPayload;
import club.fiverr.gateway.security.JwtTokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_ROLES = "X-User-Roles";

    private final JwtTokenValidator jwtTokenValidator;
    private final List<String> publicPaths;

    public JwtAuthenticationGlobalFilter(
            JwtTokenValidator jwtTokenValidator,
            @Value("${security.jwt.public-paths:/api/v1/auth,/api/v1/users,/api/v1/profiles,/api/v1/sellers,/api/v1/gigs,/api/v1/categories,/api/v1/orders,/api/v1/payments,/api/v1/wallets,/api/v1/messages,/api/v1/conversations,/api/v1/reviews,/api/v1/notifications,/api/v1/search,/actuator,/fallback}") List<String> publicPaths) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.publicPaths = publicPaths;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return writeUnauthorizedResponse(exchange, "Missing or malformed Authorization Bearer token");
        }

        String token = authHeader.substring(7).trim();
        Optional<JwtClaimsPayload> payloadOpt = jwtTokenValidator.validateAndExtract(token);
        if (payloadOpt.isEmpty()) {
            return writeUnauthorizedResponse(exchange, "Invalid or expired JWT token");
        }

        JwtClaimsPayload payload = payloadOpt.get();
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(HEADER_USER_ID, payload.userId())
                .header(HEADER_USER_EMAIL, payload.email())
                .header(HEADER_USER_ROLES, String.join(",", payload.roles()))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        if ("/".equals(path)) {
            return true;
        }
        return publicPaths.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> writeUnauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now().toString(),
                message,
                exchange.getRequest().getURI().getPath()
        );
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
