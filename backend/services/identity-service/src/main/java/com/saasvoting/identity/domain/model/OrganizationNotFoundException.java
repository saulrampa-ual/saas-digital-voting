package com.saasvoting.identity.domain.model;

/** The organization does not exist or is not visible to the caller. */
public class OrganizationNotFoundException extends DomainException {

    public OrganizationNotFoundException(String id) {
        super("Organization not found: " + id);
    }
}
