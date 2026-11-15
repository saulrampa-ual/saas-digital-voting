package com.saasvoting.gateway.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class ClientAddressKeyResolverTest {

    private static final InetSocketAddress CLIENT = new InetSocketAddress("203.0.113.7", 50_000);

    @Test
    void usesTheConnectionAddressByDefault() {
        assertEquals("203.0.113.7", resolve(new ClientAddressKeyResolver(0), null));
    }

    @Test
    void ignoresForwardedForWithoutTrustedProxies() {
        assertEquals("203.0.113.7", resolve(new ClientAddressKeyResolver(0), "198.51.100.1"));
    }

    @Test
    void usesTheAddressAddedByTheTrustedProxy() {
        // The client forged the first value; the trusted proxy appended the real address of the client
        assertEquals("198.51.100.20", resolve(new ClientAddressKeyResolver(1), "192.0.2.99, 198.51.100.20"));
    }

    @Test
    void fallsBackToTheConnectionAddressWithoutForwardedFor() {
        assertEquals("203.0.113.7", resolve(new ClientAddressKeyResolver(1), null));
    }

    @Test
    void returnsAFixedKeyWhenThereIsNoAddress() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/voter-access/x"));

        assertEquals(ClientAddressKeyResolver.UNKNOWN, new ClientAddressKeyResolver(0).resolve(exchange).block());
    }

    private static String resolve(ClientAddressKeyResolver resolver, String forwardedFor) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get("/api/v1/voter-access/x")
                .remoteAddress(CLIENT);
        if (forwardedFor != null) {
            request.header("X-Forwarded-For", forwardedFor);
        }
        return resolver.resolve(MockServerWebExchange.from(request)).block();
    }
}
