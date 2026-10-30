package com.saasvoting.ballotbox.application.port.out;

import com.saasvoting.ballotbox.domain.model.AdminAction;

/** RF-19 (D-44): administrative actions waiting to be delivered to identity-service, retried until accepted. */
public interface AdminActionOutbox {

    void append(AdminAction action);
}
