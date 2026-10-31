package com.saasvoting.voteraccess.domain.model;

/** RF-05b (D-48): how the voters of the process prove who they are after opening their personal link. */
public enum VoterAuthentication {
    /** A 6-digit code sent by email (RF-08). */
    EMAIL_CODE,
    /** The institutional account (OIDC through Keycloak) whose verified email is the one of the roll. */
    INSTITUTIONAL_SSO
}
