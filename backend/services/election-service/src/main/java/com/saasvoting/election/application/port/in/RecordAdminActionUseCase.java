package com.saasvoting.election.application.port.in;

import com.saasvoting.election.domain.model.AdminAction;

/** RF-19 (D-44): keep an administrative action until identity-service has it. */
public interface RecordAdminActionUseCase {

    void record(AdminAction action);
}
