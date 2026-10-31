package com.saasvoting.voteraccess.application.port.out;

/** RF-05b (D-48): checks the ID token of the institutional account (OIDC) and gives the email it proves. */
public interface VoterIdentityVerifier {

    /** The email and whether the identity provider verified it. */
    record VerifiedIdentity(String email, boolean emailVerified) {
    }

    /** Throws {@code InvalidIdentityTokenException} if the token is not a valid ID token for the voter client. */
    VerifiedIdentity verify(String idToken);
}
