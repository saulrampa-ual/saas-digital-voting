package com.saasvoting.gateway.ratelimit;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans of the {@code RequestRateLimiter} filter of the voter-access route (IMP-21, D-52). Being the only
 * {@code RateLimiter} and {@code KeyResolver} of the gateway, they are also the defaults of that filter; the route
 * itself is declared in {@code application.yml}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VoterAccessRateLimitProperties.class)
public class RateLimitConfiguration {

    @Bean
    TokenBucketRateLimiter voterAccessRateLimiter(VoterAccessRateLimitProperties properties) {
        return new TokenBucketRateLimiter(properties, Clock.systemUTC());
    }

    @Bean
    ClientAddressKeyResolver clientAddressKeyResolver(VoterAccessRateLimitProperties properties) {
        return new ClientAddressKeyResolver(properties.trustedProxies());
    }
}
