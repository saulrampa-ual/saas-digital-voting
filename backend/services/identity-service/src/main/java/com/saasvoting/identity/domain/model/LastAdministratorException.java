package com.saasvoting.identity.domain.model;

/** The change would leave the organization without any administrator. */
public class LastAdministratorException extends DomainException {

    public LastAdministratorException(String organizationId) {
        super("Organization would be left without administrator: " + organizationId);
    }
}
