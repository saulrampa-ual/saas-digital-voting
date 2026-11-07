package com.saasvoting.voteraccess.infrastructure.adapter.out.identity;

import com.saasvoting.voteraccess.application.port.out.VoterIdentityVerifier;
import com.saasvoting.voteraccess.domain.model.InvalidIdentityTokenException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.List;
import java.util.Objects;

/**
 * RF-05b (D-48): validates the ID token of the institutional account with the same keys and issuer as the management
 * tokens (realm verivoto of Keycloak, which brokers the identity provider of each organization) and accepts it only if
 * it was issued to the voter client ({@code aud}). The token is never stored.
 */
public class JwtVoterIdentityVerifier implements VoterIdentityVerifier {

    private final JwtDecoder decoder;
    private final String clientId;

    public JwtVoterIdentityVerifier(JwtDecoder decoder, String clientId) {
        this.decoder = Objects.requireNonNull(decoder, "decoder must not be null");
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
    }

    @Override
    public VerifiedIdentity verify(String idToken) {
        if (idToken == null || idToken.isBlank() || idToken.length() > 8192) {
            throw new InvalidIdentityTokenException("empty or too long");
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw new InvalidIdentityTokenException(e.getMessage());
        }
        List<String> audience = jwt.getAudience();
        if (audience == null || !audience.contains(clientId)) {
            throw new InvalidIdentityTokenException("audience");
        }
        String type = jwt.getClaimAsString("typ");
        if (type != null && !"ID".equals(type)) {
            throw new InvalidIdentityTokenException("not an ID token");
        }
        Boolean verified = jwt.getClaimAsBoolean("email_verified");
        return new VerifiedIdentity(jwt.getClaimAsString("email"), Boolean.TRUE.equals(verified));
    }
}
