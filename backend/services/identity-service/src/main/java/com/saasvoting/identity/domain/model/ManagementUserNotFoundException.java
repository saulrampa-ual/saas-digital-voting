package com.saasvoting.identity.domain.model;

/** The management user does not exist in the organization. */
public class ManagementUserNotFoundException extends DomainException {

    public ManagementUserNotFoundException(java.util.UUID id) {
        super("Management user not found: " + id);
    }
}
