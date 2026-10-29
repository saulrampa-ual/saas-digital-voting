package com.saasvoting.identity.domain.model;

/** An organization with this id already exists. */
public class OrganizationAlreadyExistsException extends DomainException {

    public OrganizationAlreadyExistsException(String id) {
        super("Organization already exists: " + id);
    }
}
