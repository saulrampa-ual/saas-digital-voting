package com.saasvoting.election.domain.model;

/** RF-05b (D-48): how the voters of a process prove who they are after opening their personal link. */
public enum VoterAuthentication {
    /** A 6-digit code sent to the email of the census (RF-08). Default. */
    EMAIL_CODE,
    /** The institutional account of the organization (OIDC through Keycloak); its verified email must match. */
    INSTITUTIONAL_SSO
}
