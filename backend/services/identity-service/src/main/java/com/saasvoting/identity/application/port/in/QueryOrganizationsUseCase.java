package com.saasvoting.identity.application.port.in;

import com.saasvoting.identity.application.port.PageResult;
import com.saasvoting.identity.domain.model.Caller;
import com.saasvoting.identity.domain.model.Organization;

/** RF-01: read organizations, restricted to what the caller may see (RNF-15). */
public interface QueryOrganizationsUseCase {

    PageResult<Organization> list(Caller caller, int page, int size);

    Organization get(Caller caller, String organizationId);
}
