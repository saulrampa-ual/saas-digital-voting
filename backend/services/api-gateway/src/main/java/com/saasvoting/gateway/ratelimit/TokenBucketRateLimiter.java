package com.saasvoting.gateway.ratelimit;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import reactor.core.publisher.Mono;

/**
 * In-memory token bucket used by the {@code RequestRateLimiter} filter of the voter-access route (IMP-21, D-52).
 *
 * <p>Spring Cloud Gateway only ships a Redis-based limiter for WebFlux; the MVP runs a single gateway instance, so the
 * buckets live in memory and are lost on restart. Each key (the client address, see {@link ClientAddressKeyResolver})
 * gets its own bucket. Nothing is logged and no link, code or identity is stored (RNF-11).
 */
public class TokenBucketRateLimiter implements RateLimiter<TokenBucketRateLimiter.RouteConfig> {

    static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    static final String RETRY_AFTER_HEADER = "Retry-After";

    /** A request costs {@code UNIT} units; a bucket recovers {@code refillPerMinute} units per millisecond. */
    private static final long UNIT = 60_000L;

    /** No per-route settings: the limit comes from {@link VoterAccessRateLimitProperties}. */
    public record RouteConfig() {
    }

    private final VoterAccessRateLimitProperties properties;
    private final Clock clock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(VoterAccessRateLimitProperties properties, Clock clock) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        if (!properties.enabled()) {
            return Mono.just(new Response(true, Map.of()));
        }
        long now = clock.millis();
        String key = id == null ? "" : id;
        if (!buckets.containsKey(key) && buckets.size() >= properties.maxTrackedClients()) {
            buckets.values().removeIf(bucket -> bucket.isFullAt(now));
        }
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(now));
        return Mono.just(bucket.tryConsume(now));
    }

    /** Number of client addresses currently tracked (for tests). */
    int trackedClients() {
        return buckets.size();
    }

    @Override
    public Map<String, RouteConfig> getConfig() {
        return Map.of();
    }

    @Override
    public Class<RouteConfig> getConfigClass() {
        return RouteConfig.class;
    }

    @Override
    public RouteConfig newConfig() {
        return new RouteConfig();
    }

    /** Integer arithmetic (units of 1/60000 request) so that refills are exact. */
    private final class Bucket {

        private long units;
        private long updatedAt;

        private Bucket(long now) {
            this.units = capacityUnits();
            this.updatedAt = now;
        }

        private synchronized Response tryConsume(long now) {
            refill(now);
            if (units >= UNIT) {
                units -= UNIT;
                return new Response(true, Map.of(REMAINING_HEADER, Long.toString(units / UNIT)));
            }
            long waitMillis = ceilDiv(UNIT - units, properties.refillPerMinute());
            long waitSeconds = Math.max(1, ceilDiv(waitMillis, 1000));
            return new Response(false, Map.of(REMAINING_HEADER, "0", RETRY_AFTER_HEADER, Long.toString(waitSeconds)));
        }

        private synchronized boolean isFullAt(long now) {
            refill(now);
            return units >= capacityUnits();
        }

        private void refill(long now) {
            if (now > updatedAt) {
                long elapsed = now - updatedAt;
                long missing = capacityUnits() - units;
                // Compare before multiplying: a long idle period must not overflow
                units = elapsed >= ceilDiv(missing, properties.refillPerMinute())
                        ? capacityUnits()
                        : units + elapsed * properties.refillPerMinute();
                updatedAt = now;
            }
        }

        private long capacityUnits() {
            return properties.capacity() * UNIT;
        }

        private static long ceilDiv(long dividend, long divisor) {
            return -Math.floorDiv(-dividend, divisor);
        }
    }
}
