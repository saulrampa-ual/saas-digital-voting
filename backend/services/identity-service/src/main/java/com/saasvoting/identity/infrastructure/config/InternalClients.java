package com.saasvoting.identity.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Objects;
import java.util.Set;

/**
 * IMP-26 (T-04 of the threat model): the role SERVICE is not enough on {@code /internal/**}; the token must also come
 * from the Keycloak client that uses the route (claim {@code azp}). election-service and ballot-box-service deliver
 * their administrative actions (RF-19).
 */
@ConfigurationProperties(prefix = "verivoto.internal-clients")
public record InternalClients(String election, String ballotBox) {

    public InternalClients {
        election = election == null ? "election-service" : election;
        ballotBox = ballotBox == null ? "ballot-box-service" : ballotBox;
    }

    /** Role SERVICE and one of the given clients; anything else is denied (401 without token, 403 otherwise). */
    public static AuthorizationManager<RequestAuthorizationContext> only(String... clientIds) {
        Set<String> allowed = Set.of(clientIds);
        return (authentication, context) -> new AuthorizationDecision(isClient(authentication.get(), allowed));
    }

    private static boolean isClient(Authentication authentication, Set<String> allowed) {
        if (!(authentication instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) {
            return false;
        }
        boolean service = token.getAuthorities().stream().anyMatch(a -> "ROLE_SERVICE".equals(a.getAuthority()));
        String client = Objects.requireNonNullElse(token.getToken().getClaimAsString("azp"),
                Objects.requireNonNullElse(token.getToken().getClaimAsString("client_id"), ""));
        return service && allowed.contains(client);
    }
}
