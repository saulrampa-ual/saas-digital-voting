package com.saasvoting.gateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Per-IP limit of the public voter endpoints {@code /api/v1/voter-access/**} (IMP-21, D-52, threat T-22).
 *
 * <p>Token bucket per client address: a client may send {@code capacity} requests in a burst and gets
 * {@code refillPerMinute} new requests per minute. The limit lives in the memory of the gateway (a single instance in
 * the MVP) and never stores links, codes or identities.
 *
 * @param enabled           {@code false} turns the limit off (for example, for the load test H1c from one machine)
 * @param capacity          maximum burst of requests per client address
 * @param refillPerMinute   requests per minute a client address recovers
 * @param trustedProxies    number of trusted proxies in front of the gateway that append to {@code X-Forwarded-For};
 *                          {@code 0} (default) ignores that header, which any client could forge
 * @param maxTrackedClients upper bound of client addresses kept in memory; full buckets are dropped first
 */
@ConfigurationProperties("verivoto.gateway.voter-access-rate-limit")
public record VoterAccessRateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("120") int capacity,
        @DefaultValue("120") int refillPerMinute,
        @DefaultValue("0") int trustedProxies,
        @DefaultValue("50000") int maxTrackedClients) {

    public VoterAccessRateLimitProperties {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        if (refillPerMinute < 1) {
            throw new IllegalArgumentException("refillPerMinute must be at least 1");
        }
        if (trustedProxies < 0) {
            throw new IllegalArgumentException("trustedProxies must not be negative");
        }
        if (maxTrackedClients < 1) {
            throw new IllegalArgumentException("maxTrackedClients must be at least 1");
        }
    }
}
