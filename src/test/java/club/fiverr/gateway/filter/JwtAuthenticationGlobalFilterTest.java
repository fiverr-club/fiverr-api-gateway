package club.fiverr.gateway.filter;

import club.fiverr.gateway.security.JwtClaimsPayload;
import club.fiverr.gateway.security.JwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationGlobalFilterTest {

    @Mock
    private JwtTokenValidator jwtTokenValidator;

    @Mock
    private GatewayFilterChain filterChain;

    private JwtAuthenticationGlobalFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationGlobalFilter(
                jwtTokenValidator,
                List.of("/api/v1/auth/login", "/api/v1/auth/register", "/actuator")
        );
    }

    @Test
    void shouldAllowPublicPathWithoutAuthorizationHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(exchange);
        verifyNoInteractions(jwtTokenValidator);
    }

    @Test
    void shouldReturnUnauthorizedWhenProtectedPathMissingHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders/my").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldEnrichDownstreamHeadersWhenTokenIsValid() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-jwt")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        JwtClaimsPayload payload = new JwtClaimsPayload("user-123", "alice@fiverr.club", List.of("BUYER", "SELLER"));
        when(jwtTokenValidator.validateAndExtract("valid-jwt")).thenReturn(Optional.of(payload));

        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        when(filterChain.filter(any())).thenAnswer(inv -> {
            captured.set(inv.getArgument(0));
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertNotNull(captured.get());
        HttpHeaders downstreamHeaders = captured.get().getRequest().getHeaders();
        assertEquals("user-123", downstreamHeaders.getFirst(JwtAuthenticationGlobalFilter.HEADER_USER_ID));
        assertEquals("alice@fiverr.club", downstreamHeaders.getFirst(JwtAuthenticationGlobalFilter.HEADER_USER_EMAIL));
        assertEquals("BUYER,SELLER", downstreamHeaders.getFirst(JwtAuthenticationGlobalFilter.HEADER_USER_ROLES));
    }
}
