package com.saasvoting.identity.application.service;

import com.saasvoting.identity.application.port.PageResult;
import com.saasvoting.identity.application.port.in.QueryAdminActionsUseCase;
import com.saasvoting.identity.application.port.in.RecordAdminActionUseCase;
import com.saasvoting.identity.application.port.out.AdminActionRepository;
import com.saasvoting.identity.domain.model.AdminAction;
import com.saasvoting.identity.domain.model.AdminActionFilter;
import com.saasvoting.identity.domain.model.Caller;
import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.domain.model.OperationNotAllowedException;

import java.util.List;
import java.util.Objects;

/** RF-19: record and query the administrative actions of an organization (RI-13, D-44). */
public final class AdminActionService implements RecordAdminActionUseCase, QueryAdminActionsUseCase {

    private final AdminActionRepository actions;

    public AdminActionService(AdminActionRepository actions) {
        this.actions = Objects.requireNonNull(actions, "actions must not be null");
    }

    @Override
    public void record(AdminAction action) {
        actions.appendIfAbsent(Objects.requireNonNull(action, "action must not be null"));
    }

    @Override
    public PageResult<AdminAction> search(Caller caller, AdminActionFilter filter, int page, int size) {
        return actions.search(organizationOf(caller), filter == null ? AdminActionFilter.none() : filter, page, size);
    }

    @Override
    public List<Actor> actors(Caller caller) {
        return actions.actors(organizationOf(caller));
    }

    /** Only ADMIN and AUDITOR, always inside their organization (RF-19 comments, RNF-15). */
    private static String organizationOf(Caller caller) {
        if (caller.organizationId().isEmpty()
                || !(caller.hasRole(ManagementRole.ADMIN) || caller.hasRole(ManagementRole.AUDITOR))) {
            throw new OperationNotAllowedException("view administrative actions");
        }
        return caller.organizationId().get();
    }
}
