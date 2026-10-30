package com.saasvoting.ballotbox.infrastructure.config;

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
 * AUDITOR (export: ADMIN, RF-15). Internal endpoints need a Keycloak client-credentials token with role SERVICE
 * (D-22). Everything else is denied.
 */
@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/v1/ballot-box/processes/*/results/export")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/ballot-box/processes/*/results",
                                "/api/v1/ballot-box/processes/*/results/counted-votes")
                        .hasAnyRole("ADMIN", "AUDITOR")
                        .requestMatchers("/api/v1/ballot-box/processes/*/results/**").denyAll()
                        .requestMatchers("/api/v1/ballot-box/**").permitAll()
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/internal/**").hasRole("SERVICE")
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
