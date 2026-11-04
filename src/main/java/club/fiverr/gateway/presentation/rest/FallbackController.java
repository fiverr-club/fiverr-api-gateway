package club.fiverr.gateway.presentation.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/{serviceName}")
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
