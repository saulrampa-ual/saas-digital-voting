package com.saasvoting.voteraccess.infrastructure.adapter.out.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saasvoting.voteraccess.application.port.out.VoterIdentityVerifier.VerifiedIdentity;
import com.saasvoting.voteraccess.domain.model.InvalidIdentityTokenException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * P7.1 (A5), RF-05b (D-48): the ID token of the institutional account is accepted only if the decoder validates it,
 * it was issued to the voter client and it is an ID token; the e-mail counts as verified only when the identity
 * provider says so. Synthetic tokens only: the decoder is a mock, so no key or real account is involved.
 */
class JwtVoterIdentityVerifierTest {

    private static final String CLIENT = "verivoto-voter";
    private final JwtDecoder decoder = mock(JwtDecoder.class);
    private final JwtVoterIdentityVerifier verifier = new JwtVoterIdentityVerifier(decoder, CLIENT);

    @Test
    void acceptsAnIdTokenForTheVoterClientWithAVerifiedEmail() {
        when(decoder.decode("token")).thenReturn(jwt(List.of(CLIENT, "account"),
                Map.of("typ", "ID", "email", "p01@verivoto.test", "email_verified", true)));

        VerifiedIdentity identity = verifier.verify("token");

        assertEquals("p01@verivoto.test", identity.email());
        assertTrue(identity.emailVerified());
    }

    @Test
    void anEmailIsNotVerifiedWhenTheClaimIsFalseOrMissing() {
        when(decoder.decode("false")).thenReturn(jwt(List.of(CLIENT),
                Map.of("typ", "ID", "email", "p01@verivoto.test", "email_verified", false)));
        when(decoder.decode("missing")).thenReturn(jwt(List.of(CLIENT), Map.of("email", "p01@verivoto.test")));

        assertFalse(verifier.verify("false").emailVerified());
        assertFalse(verifier.verify("missing").emailVerified());
    }

    @Test
    void rejectsATokenIssuedToAnotherClient() {
        when(decoder.decode("token")).thenReturn(jwt(List.of("verivoto-admin"),
                Map.of("typ", "ID", "email", "p01@verivoto.test", "email_verified", true)));

        assertThrows(InvalidIdentityTokenException.class, () -> verifier.verify("token"));
    }

    @Test
    void rejectsAnAccessTokenEvenForTheVoterClient() {
        when(decoder.decode("token")).thenReturn(jwt(List.of(CLIENT),
                Map.of("typ", "Bearer", "email", "p01@verivoto.test", "email_verified", true)));

        assertThrows(InvalidIdentityTokenException.class, () -> verifier.verify("token"));
    }

    @Test
    void rejectsATokenTheDecoderDoesNotAccept() {
        when(decoder.decode("token")).thenThrow(new BadJwtException("signature"));

        assertThrows(InvalidIdentityTokenException.class, () -> verifier.verify("token"));
    }

    @Test
    void rejectsEmptyOrOversizedTokensWithoutDecodingThem() {
        assertThrows(InvalidIdentityTokenException.class, () -> verifier.verify(null));
        assertThrows(InvalidIdentityTokenException.class, () -> verifier.verify("  "));
        assertThrows(InvalidIdentityTokenException.class, () -> verifier.verify("x".repeat(8193)));
        verify(decoder, never()).decode(anyString());
    }

    private static Jwt jwt(List<String> audience, Map<String, Object> claims) {
        Instant now = Instant.parse("2026-11-08T12:00:00Z");
        return Jwt.withTokenValue("synthetic")
                .header("alg", "RS256")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .audience(audience)
                .claims(values -> values.putAll(claims))
                .build();
    }
}
