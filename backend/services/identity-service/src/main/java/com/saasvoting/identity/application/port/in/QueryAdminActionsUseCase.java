package com.saasvoting.identity.application.port.in;

import com.saasvoting.identity.application.port.PageResult;
import com.saasvoting.identity.domain.model.AdminAction;
import com.saasvoting.identity.domain.model.AdminActionFilter;
import com.saasvoting.identity.domain.model.Caller;

import java.util.List;
import java.util.UUID;

/**
 * RF-19: the administrator and the auditor see the actions of their own organization, newest first (RNF-15). Other
 * roles and users without organization are not allowed.
 */
public interface QueryAdminActionsUseCase {

    record Actor(UUID actorId, String actorName) {
    }

    PageResult<AdminAction> search(Caller caller, AdminActionFilter filter, int page, int size);

    /** People who performed actions in the organization, for the "user" filter. */
    List<Actor> actors(Caller caller);
}
