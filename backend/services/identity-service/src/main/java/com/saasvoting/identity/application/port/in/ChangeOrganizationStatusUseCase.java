package com.saasvoting.identity.application.port.in;

import com.saasvoting.identity.domain.model.Caller;
import com.saasvoting.identity.domain.model.Organization;

/** RF-01: suspend or reactivate an organization. */
public interface ChangeOrganizationStatusUseCase {

    Organization changeStatus(Caller caller, String organizationId, Organization.Status status);
}
