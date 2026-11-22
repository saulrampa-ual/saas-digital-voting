package com.saasvoting.gateway.server;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.reactor.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * IMP-57 / H1c: length of the queue of connections waiting to be accepted by the gateway ({@code SO_BACKLOG}).
 *
 * <p>In the load test of 11/11/2026 more than half of 500 simultaneous connections were refused by the gateway before
 * reaching any service, which matches the default queue of Netty (the {@code SOMAXCONN} of the system, 200 on
 * Windows). A longer queue lets those connections wait instead of being refused. It changes nothing in routing,
 * security or what is logged. The operating system may cap the value.
 */
@Configuration(proxyBeanMethods = false)
public class ConnectionBacklogConfiguration {

    @Bean
    NettyServerCustomizer connectionBacklogCustomizer(@Value("${verivoto.gateway.server.backlog:1024}") int backlog) {
        if (backlog < 1) {
            throw new IllegalArgumentException("verivoto.gateway.server.backlog must be at least 1");
        }
        return server -> server.option(ChannelOption.SO_BACKLOG, backlog);
    }
}
