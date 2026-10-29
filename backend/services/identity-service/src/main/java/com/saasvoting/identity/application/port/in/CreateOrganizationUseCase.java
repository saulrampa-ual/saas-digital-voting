package com.saasvoting.identity.application.port.in;

import com.saasvoting.identity.domain.model.Caller;
import com.saasvoting.identity.domain.model.Organization;
import com.saasvoting.identity.domain.model.PersonData;

/** RF-01: register an organization together with its initial administrator. */
public interface CreateOrganizationUseCase {

    Organization create(Caller caller, String id, String name, PersonData initialAdministrator);
}
