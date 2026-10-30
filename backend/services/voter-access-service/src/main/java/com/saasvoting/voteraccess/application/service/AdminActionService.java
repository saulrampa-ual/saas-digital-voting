package com.saasvoting.voteraccess.application.service;

import com.saasvoting.voteraccess.application.port.in.RecordAdminActionUseCase;
import com.saasvoting.voteraccess.application.port.out.AdminActionOutbox;
import com.saasvoting.voteraccess.domain.model.AdminAction;

import java.util.Objects;

/** RF-19 (D-44): administrative actions go to the outbox; identity-service keeps them for the traceability. */
public final class AdminActionService implements RecordAdminActionUseCase {

    private final AdminActionOutbox outbox;

    public AdminActionService(AdminActionOutbox outbox) {
        this.outbox = Objects.requireNonNull(outbox, "outbox must not be null");
    }

    @Override
    public void record(AdminAction action) {
        outbox.append(Objects.requireNonNull(action, "action must not be null"));
    }
}
