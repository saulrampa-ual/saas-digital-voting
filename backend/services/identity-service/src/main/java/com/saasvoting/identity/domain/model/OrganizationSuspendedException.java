package com.saasvoting.identity.domain.model;

/** RF-21: a suspended organization cannot create API keys. */
public class OrganizationSuspendedException extends DomainException {

    public OrganizationSuspendedException(String organizationId) {
        super("Organization suspended: " + organizationId);
    }
}
