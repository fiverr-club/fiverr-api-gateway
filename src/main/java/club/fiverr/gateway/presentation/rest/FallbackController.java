package club.fiverr.gateway.presentation.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
public class FallbackController {

    @GetMapping("/")
    public Mono<ResponseEntity<Map<String, Object>>> gatewayStatus() {
        Map<String, Object> payload = Map.of(
                "service", "fiverr-api-gateway",
                "brand", "FIVERREX",
                "status", "UP",
                "swaggerUi", "http://localhost:8080/swagger-ui/index.html",
                "openApiDocs", "http://localhost:8080/v3/api-docs",
                "timestamp", Instant.now().toString(),
                "routes", List.of(
                        "/api/v1/auth/**",
                        "/api/v1/users/**",
                        "/api/v1/sellers/**",
                        "/api/v1/gigs/**",
                        "/api/v1/orders/**",
                        "/api/v1/payments/**",
                        "/api/v1/messages/**",
                        "/api/v1/reviews/**",
                        "/api/v1/notifications/**"
                )
        );
        return Mono.just(ResponseEntity.ok(payload));
    }

    @GetMapping("/fallback/{serviceName}")
    public Mono<ResponseEntity<Map<String, Object>>> serviceFallback(@PathVariable String serviceName) {
        Map<String, Object> payload = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "service", serviceName,
                "message", "Upstream microservice '" + serviceName + "' is temporarily unavailable. Please retry shortly."
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(payload));
    }
}
