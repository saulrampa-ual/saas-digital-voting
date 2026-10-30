package com.saasvoting.identity.application.port.out;

import com.saasvoting.identity.application.port.PageResult;
import com.saasvoting.identity.application.port.in.QueryAdminActionsUseCase.Actor;
import com.saasvoting.identity.domain.model.AdminAction;
import com.saasvoting.identity.domain.model.AdminActionFilter;

import java.util.List;

/** Append-only store of administrative actions (RNF-13): no update and no delete. */
public interface AdminActionRepository {

    /** Does nothing if an action with the same event id is already stored. */
    void appendIfAbsent(AdminAction action);

    PageResult<AdminAction> search(String organizationId, AdminActionFilter filter, int page, int size);

    List<Actor> actors(String organizationId);
}
