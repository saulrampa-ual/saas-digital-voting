package com.saasvoting.voteraccess.application.port.out;

import com.saasvoting.voteraccess.domain.model.Notification;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository {

    /** PENDING notifications whose next attempt is due, oldest first. */
    List<Notification> findDue(Instant now, int limit);

    void save(Notification notification);
}
