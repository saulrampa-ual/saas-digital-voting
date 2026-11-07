package com.saasvoting.election.domain.model;

/** RF-18: voter-access did not answer, so the export would be incomplete. */
public class PrivacyServiceUnavailableException extends DomainException {

    public PrivacyServiceUnavailableException(String reason) {
        super("voter-access unavailable: " + reason);
    }
}
