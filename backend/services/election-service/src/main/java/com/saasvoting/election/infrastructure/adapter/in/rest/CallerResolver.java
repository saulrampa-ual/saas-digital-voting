package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.domain.model.Caller;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Builds the domain {@link Caller} from the Keycloak access token (claims {@code roles} and {@code organization_id}). */
final class CallerResolver {

    private CallerResolver() {
    }

    static Caller from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        String organizationId = jwt.getClaimAsString("organization_id");
        return new Caller(
                UUID.fromString(jwt.getSubject()),
                Optional.ofNullable(organizationId).filter(value -> !value.isBlank()),
                roles == null ? Set.of() : new HashSet<>(roles));
    }
}
