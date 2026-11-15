package com.saasvoting.gateway.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter.Response;

class TokenBucketRateLimiterTest {

    private static final String ROUTE = "voter-access";

    private final MutableClock clock = new MutableClock();

    @Test
    void allowsTheBurstAndThenAsksToWait() {
        TokenBucketRateLimiter limiter = limiter(true, 3, 60, 100);

        for (int i = 0; i < 3; i++) {
            assertTrue(call(limiter, "203.0.113.7").isAllowed());
        }
        Response denied = call(limiter, "203.0.113.7");

        assertFalse(denied.isAllowed());
        assertEquals("0", denied.getHeaders().get(TokenBucketRateLimiter.REMAINING_HEADER));
        assertEquals("1", denied.getHeaders().get(TokenBucketRateLimiter.RETRY_AFTER_HEADER));
    }

    @Test
    void reportsTheRemainingRequests() {
        TokenBucketRateLimiter limiter = limiter(true, 3, 60, 100);

        assertEquals("2", call(limiter, "203.0.113.7").getHeaders().get(TokenBucketRateLimiter.REMAINING_HEADER));
        assertEquals("1", call(limiter, "203.0.113.7").getHeaders().get(TokenBucketRateLimiter.REMAINING_HEADER));
    }

    @Test
    void recoversRequestsOverTime() {
        TokenBucketRateLimiter limiter = limiter(true, 2, 6, 100); // one request every 10 s
        call(limiter, "203.0.113.7");
        call(limiter, "203.0.113.7");
        Response denied = call(limiter, "203.0.113.7");
        assertFalse(denied.isAllowed());
        assertEquals("10", denied.getHeaders().get(TokenBucketRateLimiter.RETRY_AFTER_HEADER));

        clock.advanceMillis(9_999);
        assertFalse(call(limiter, "203.0.113.7").isAllowed());
        clock.advanceMillis(1);
        assertTrue(call(limiter, "203.0.113.7").isAllowed());
        assertFalse(call(limiter, "203.0.113.7").isAllowed());
    }

    @Test
    void neverRecoversMoreThanTheBurst() {
        TokenBucketRateLimiter limiter = limiter(true, 2, 60, 100);
        clock.advanceMillis(3_600_000);

        assertTrue(call(limiter, "203.0.113.7").isAllowed());
        assertTrue(call(limiter, "203.0.113.7").isAllowed());
        assertFalse(call(limiter, "203.0.113.7").isAllowed());
    }

    @Test
    void keepsOneBucketPerClientAddress() {
        TokenBucketRateLimiter limiter = limiter(true, 1, 1, 100);

        assertTrue(call(limiter, "203.0.113.7").isAllowed());
        assertFalse(call(limiter, "203.0.113.7").isAllowed());
        assertTrue(call(limiter, "198.51.100.20").isAllowed());
    }

    @Test
    void allowsEverythingWhenDisabled() {
        TokenBucketRateLimiter limiter = limiter(false, 1, 1, 100);

        for (int i = 0; i < 10; i++) {
            Response response = call(limiter, "203.0.113.7");
            assertTrue(response.isAllowed());
            assertTrue(response.getHeaders().isEmpty());
        }
        assertEquals(0, limiter.trackedClients());
    }

    @Test
    void dropsFullBucketsWhenTheTrackingLimitIsReached() {
        TokenBucketRateLimiter limiter = limiter(true, 2, 60, 2);
        call(limiter, "203.0.113.1");
        call(limiter, "203.0.113.2");
        clock.advanceMillis(2_000); // both buckets full again

        call(limiter, "203.0.113.3");

        assertEquals(1, limiter.trackedClients());
    }

    @Test
    void keepsBucketsThatAreStillLimited() {
        TokenBucketRateLimiter limiter = limiter(true, 2, 60, 2);
        call(limiter, "203.0.113.1");
        call(limiter, "203.0.113.1");
        call(limiter, "203.0.113.2");

        call(limiter, "203.0.113.3");

        assertEquals(3, limiter.trackedClients());
        assertFalse(call(limiter, "203.0.113.1").isAllowed());
    }

    @Test
    void rejectsInvalidSettings() {
        assertThrows(IllegalArgumentException.class, () -> new VoterAccessRateLimitProperties(true, 0, 60, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> new VoterAccessRateLimitProperties(true, 1, 0, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> new VoterAccessRateLimitProperties(true, 1, 1, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> new VoterAccessRateLimitProperties(true, 1, 1, 0, 0));
    }

    private TokenBucketRateLimiter limiter(boolean enabled, int capacity, int perMinute, int maxClients) {
        return new TokenBucketRateLimiter(
                new VoterAccessRateLimitProperties(enabled, capacity, perMinute, 0, maxClients), clock);
    }

    private static Response call(TokenBucketRateLimiter limiter, String address) {
        return limiter.isAllowed(ROUTE, address).block();
    }

    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-11-05T09:00:00Z");

        void advanceMillis(long millis) {
            now = now.plusMillis(millis);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
