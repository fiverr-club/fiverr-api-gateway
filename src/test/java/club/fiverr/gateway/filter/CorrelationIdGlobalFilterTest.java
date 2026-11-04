package club.fiverr.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CorrelationIdGlobalFilterTest {

    private final CorrelationIdGlobalFilter filter = new CorrelationIdGlobalFilter();

    @Test
    void shouldGenerateCorrelationIdWhenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/gigs/public").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        String responseCorrelationId = exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER);
        assertNotNull(responseCorrelationId);
        assertFalse(responseCorrelationId.isBlank());
    }

    @Test
    void shouldPreserveExistingCorrelationId() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/gigs/public")
                .header(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER, "trace-abc-999")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        String responseCorrelationId = exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER);
        assertEquals("trace-abc-999", responseCorrelationId);
    }
}
