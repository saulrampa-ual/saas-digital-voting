package com.saasvoting.gateway.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * IMP-21 (D-52): the voter-access route answers 429 once a client address spends its burst; other routes are not
 * limited. The services behind the gateway are not running (closed port), so allowed requests end in a gateway error,
 * never in 429.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "VOTER_ACCESS_SERVICE_URI=http://127.0.0.1:9",
        "BALLOT_BOX_SERVICE_URI=http://127.0.0.1:9",
        // Pinned so the test does not depend on the shell (start-services.ps1 -NoRateLimit sets the variable to false)
        "verivoto.gateway.voter-access-rate-limit.enabled=true",
        "verivoto.gateway.voter-access-rate-limit.capacity=2",
        "verivoto.gateway.voter-access-rate-limit.refill-per-minute=1"})
class VoterAccessRateLimitIntegrationTest {

    private static final int TOO_MANY_REQUESTS = 429;

    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build();

    @Value("${local.server.port}")
    private int port;

    @Test
    void limitsTheVoterAccessRouteByClientAddress() throws Exception {
        assertNotEquals(TOO_MANY_REQUESTS, get("/api/v1/voter-access/links/synthetic-link").statusCode());
        assertNotEquals(TOO_MANY_REQUESTS, get("/api/v1/voter-access/links/synthetic-link").statusCode());

        HttpResponse<String> limited = get("/api/v1/voter-access/links/synthetic-link");

        assertEquals(TOO_MANY_REQUESTS, limited.statusCode());
        assertEquals("0", limited.headers().firstValue("X-RateLimit-Remaining").orElseThrow());
        assertTrue(Long.parseLong(limited.headers().firstValue("Retry-After").orElseThrow()) > 0);

        // Other public routes keep working for the same client address
        assertNotEquals(TOO_MANY_REQUESTS, get("/api/v1/ballot-box/processes/synthetic/results").statusCode());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
