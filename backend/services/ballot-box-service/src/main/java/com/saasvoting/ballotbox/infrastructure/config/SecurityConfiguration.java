package com.saasvoting.ballotbox.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Voter endpoints are public: the single-use voting token is the credential (RF-09). Fingerprints and the public
 * results link are public too (ADR-0007, D-16). Results of an organization need a management token with ADMIN or
 * AUDITOR (export and minutes: ADMIN, RF-15, RF-16); the chain of the ballot box only to the AUDITOR (RF-12). Internal endpoints need a Keycloak client-credentials token with role SERVICE
 * (D-22). Everything else is denied.
 */
@Configuration
@EnableConfigurationProperties(InternalClients.class)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter, InternalClients clients)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/v1/ballot-box/processes/*/results/export",
                                "/api/v1/ballot-box/processes/*/results/minutes")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/ballot-box/processes/*/results",
                                "/api/v1/ballot-box/processes/*/results/counted-votes")
                        .hasAnyRole("ADMIN", "AUDITOR")
                        .requestMatchers("/api/v1/ballot-box/processes/*/results/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/ballot-box/processes/*/audit-events",
                                "/api/v1/ballot-box/processes/*/audit-events/verification")
                        .hasRole("AUDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/ballot-box/processes/*/audit-events/verification")
                        .hasRole("AUDITOR")
                        .requestMatchers("/api/v1/ballot-box/processes/*/audit-events/**").denyAll()
                        .requestMatchers("/api/v1/ballot-box/**").permitAll()
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        // IMP-26: ballots and statuses only from election-service
                        .requestMatchers("/internal/v1/processes/**").access(InternalClients.only(clients.election()))
                        .requestMatchers("/internal/**").denyAll()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter rolesConverter = new JwtGrantedAuthoritiesConverter();
        rolesConverter.setAuthoritiesClaimName("roles");
        rolesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(rolesConverter);
        return authenticationConverter;
    }
}
