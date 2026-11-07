package com.saasvoting.voteraccess.application.port.in;

import com.saasvoting.voteraccess.domain.model.AdminAction;

/** RF-19 (D-44): keep an administrative action until identity-service has it. */
public interface RecordAdminActionUseCase {

    void record(AdminAction action);
}
