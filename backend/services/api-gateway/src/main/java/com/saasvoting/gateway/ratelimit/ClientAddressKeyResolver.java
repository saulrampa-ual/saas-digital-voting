package com.saasvoting.gateway.ratelimit;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.support.ipresolver.RemoteAddressResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Key of the voter-access limit: the IP address of the client (IMP-21, D-52).
 *
 * <p>By default it is the address of the TCP connection. {@code X-Forwarded-For} is only read when the gateway runs
 * behind {@code trustedProxies} known proxies; then the address added by the outermost trusted proxy is used, so a
 * client cannot pick its own key by sending the header. In local development every browser request arrives through
 * the Angular development proxy, so all of them share the key {@code 127.0.0.1}.
 */
public class ClientAddressKeyResolver implements KeyResolver {

    static final String UNKNOWN = "unknown";

    private final RemoteAddressResolver resolver;

    public ClientAddressKeyResolver(int trustedProxies) {
        this.resolver = trustedProxies > 0
                ? XForwardedRemoteAddressResolver.maxTrustedIndex(trustedProxies)
                : new RemoteAddressResolver() {
                };
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        InetSocketAddress address = resolver.resolve(exchange);
        if (address == null) {
            return Mono.just(UNKNOWN);
        }
        InetAddress ip = address.getAddress();
        return Mono.just(ip != null ? ip.getHostAddress() : address.getHostString());
    }
}
