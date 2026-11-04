package club.fiverr.gateway.presentation.rest;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
public class SwaggerUiController {

    private static final String SWAGGER_UI_HTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>FIVERREX. API Gateway — Swagger UI</title>
              <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5.17.14/swagger-ui.css" />
              <style>
                body { margin: 0; background: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
                .fiverrex-topbar {
                  background: linear-gradient(90deg, #0a4226 0%, #0d3b24 100%);
                  color: #fff;
                  padding: 12px 24px;
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  border-bottom: 2px solid #1dbf73;
                }
                .fiverrex-brand { font-size: 22px; font-weight: 800; letter-spacing: -0.5px; }
                .fiverrex-brand span { color: #1dbf73; }
                .fiverrex-badge {
                  background: rgba(29, 191, 115, 0.18);
                  border: 1px solid rgba(29, 191, 115, 0.45);
                  color: #a7f3d0;
                  padding: 4px 12px;
                  border-radius: 9999px;
                  font-size: 12px;
                  font-weight: 600;
                }
                .swagger-ui .topbar { display: none; }
              </style>
            </head>
            <body>
              <div class="fiverrex-topbar">
                <div class="fiverrex-brand">FIVERREX<span>.</span> API Gateway</div>
                <div class="fiverrex-badge">OpenAPI 3.0 • 9 Microservices Connected (:8080 — :8088)</div>
              </div>
              <div id="swagger-ui"></div>
              <script src="https://unpkg.com/swagger-ui-dist@5.17.14/swagger-ui-bundle.js"></script>
              <script src="https://unpkg.com/swagger-ui-dist@5.17.14/swagger-ui-standalone-preset.js"></script>
              <script>
                window.onload = function() {
                  window.ui = SwaggerUIBundle({
                    url: "/v3/api-docs",
                    dom_id: "#swagger-ui",
                    deepLinking: true,
                    displayRequestDuration: true,
                    filter: true,
                    tryItOutEnabled: true,
                    persistAuthorization: true,
                    presets: [
                      SwaggerUIBundle.presets.apis,
                      SwaggerUIStandalonePreset
                    ],
                    layout: "StandaloneLayout"
                  });
                };
              </script>
            </body>
            </html>
            """;

    private final String openApiJson;

    public SwaggerUiController() {
        ClassPathResource resource = new ClassPathResource("openapi/fiverrex-gateway-openapi.json");
        try (InputStream is = resource.getInputStream()) {
            this.openApiJson = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load openapi/fiverrex-gateway-openapi.json", e);
        }
    }

    @GetMapping(value = "/swagger-ui/index.html", produces = MediaType.TEXT_HTML_VALUE)
    public Mono<ResponseEntity<String>> swaggerUiIndex() {
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(SWAGGER_UI_HTML));
    }

    @GetMapping({"/swagger-ui", "/swagger-ui/", "/swagger-ui.html"})
    public Mono<Void> redirectToSwaggerUi(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(URI.create("/swagger-ui/index.html"));
        return response.setComplete();
    }

    @GetMapping(value = "/v3/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> openApiDocs() {
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(openApiJson));
    }
}

