package com.saasvoting.voteraccess.infrastructure.config;

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
 * Voter endpoints are public: the personal link and the code identify the voter (RF-08). The participation of a
 * process needs a management token with ADMIN (RF-14). Internal endpoints need a Keycloak client-credentials token
 * with role SERVICE (D-22). Everything else is denied.
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
                        .requestMatchers(HttpMethod.GET, "/api/v1/voter-access/processes/*/participation").hasRole("ADMIN")
                        .requestMatchers("/api/v1/voter-access/processes/**").denyAll()
                        .requestMatchers("/api/v1/voter-access/**").permitAll()
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        // IMP-26: each internal route only for the service that uses it
                        .requestMatchers(HttpMethod.GET, "/internal/v1/processes/*/voted-count")
                        .access(InternalClients.only(clients.ballotBox()))
                        .requestMatchers("/internal/v1/processes/**").access(InternalClients.only(clients.election()))
                        .requestMatchers("/internal/v1/voting-tokens/**", "/internal/v1/results-tokens/**")
                        .access(InternalClients.only(clients.ballotBox()))
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
