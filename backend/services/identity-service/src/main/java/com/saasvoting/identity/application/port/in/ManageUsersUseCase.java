package com.saasvoting.identity.application.port.in;

import com.saasvoting.identity.application.port.PageResult;
import com.saasvoting.identity.domain.model.Caller;
import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.domain.model.ManagementUser;
import com.saasvoting.identity.domain.model.PersonData;

import java.util.UUID;

/** RF-02: management users and roles of an organization. */
public interface ManageUsersUseCase {

    PageResult<ManagementUser> list(Caller caller, String organizationId, int page, int size);

    ManagementUser create(Caller caller, String organizationId, PersonData person, ManagementRole role);

    ManagementUser changeRole(Caller caller, String organizationId, UUID userId, ManagementRole role);
}
