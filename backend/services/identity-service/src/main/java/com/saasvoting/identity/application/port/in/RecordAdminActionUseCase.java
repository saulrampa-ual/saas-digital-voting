package com.saasvoting.identity.application.port.in;

import com.saasvoting.identity.domain.model.AdminAction;

/** RF-19: append an administrative action. Idempotent by {@code eventId}. */
public interface RecordAdminActionUseCase {

    void record(AdminAction action);
}
