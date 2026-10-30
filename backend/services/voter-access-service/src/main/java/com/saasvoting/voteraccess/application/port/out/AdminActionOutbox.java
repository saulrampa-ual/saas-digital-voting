package com.saasvoting.voteraccess.application.port.out;

import com.saasvoting.voteraccess.domain.model.AdminAction;

/** RF-19 (D-44): administrative actions waiting to be delivered to identity-service, retried until accepted. */
public interface AdminActionOutbox {

    void append(AdminAction action);
}
