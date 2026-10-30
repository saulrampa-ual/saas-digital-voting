package com.saasvoting.voteraccess.infrastructure.adapter.in.scheduling;

import com.saasvoting.voteraccess.application.port.in.ManageNotificationsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** RF-17 (D-45): releases the reminders whose time has come; the emails go out with the invitations. */
@Component
public class ReminderScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ManageNotificationsUseCase notifications;

    public ReminderScheduler(ManageNotificationsUseCase notifications) {
        this.notifications = Objects.requireNonNull(notifications, "notifications must not be null");
    }

    @Scheduled(fixedDelayString = "${verivoto.access.invitation-interval:PT10S}", initialDelayString = "PT7S")
    public void run() {
        int released = notifications.releaseDueReminders();
        if (released > 0) {
            LOG.info("Reminders released: {}", released);
        }
    }
}
