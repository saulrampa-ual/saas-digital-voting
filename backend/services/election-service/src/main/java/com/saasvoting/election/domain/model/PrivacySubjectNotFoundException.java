package com.saasvoting.election.domain.model;

/** RF-18: nobody in the censuses of the organization has this email or phone. */
public class PrivacySubjectNotFoundException extends DomainException {

    public PrivacySubjectNotFoundException() {
        super("Privacy subject not found");
    }
}
