package com.saasvoting.identity.infrastructure.config;

import com.saasvoting.identity.application.port.out.IdentityProvider;
import com.saasvoting.identity.application.port.out.ManagementUserRepository;
import com.saasvoting.identity.application.port.out.OrganizationRepository;
import com.saasvoting.identity.application.service.ManagementUserService;
import com.saasvoting.identity.application.service.OrganizationService;
import com.saasvoting.identity.infrastructure.adapter.out.identityprovider.KeycloakAdminClient;
import com.saasvoting.identity.infrastructure.adapter.out.identityprovider.KeycloakAdminProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;

/** Wires use cases and outbound adapters. Application services carry no Spring annotations. */
@Configuration
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class IdentityServiceConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    IdentityProvider keycloakIdentityProvider(KeycloakAdminProperties properties, Clock clock) {
        RestClient restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
        return new KeycloakAdminClient(restClient, properties, clock);
    }

    @Bean
    OrganizationService organizationService(
            OrganizationRepository organizations,
            ManagementUserRepository users,
            IdentityProvider identityProvider,
            Clock clock) {
        return new OrganizationService(organizations, users, identityProvider, clock);
    }

    @Bean
    ManagementUserService managementUserService(
            OrganizationRepository organizations,
            ManagementUserRepository users,
            IdentityProvider identityProvider,
            Clock clock) {
        return new ManagementUserService(organizations, users, identityProvider, clock);
    }
}
