package com.saasvoting.voteraccess.infrastructure.adapter.in.scheduling;

import com.saasvoting.voteraccess.application.port.in.SendInvitationsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Sends the pending emails (invitations, reminders, closing notices) every {@code verivoto.access.invitation-interval} (10 s by default). */
@Component
public class InvitationScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(InvitationScheduler.class);

    private final SendInvitationsUseCase invitations;

    public InvitationScheduler(SendInvitationsUseCase invitations) {
        this.invitations = Objects.requireNonNull(invitations, "invitations must not be null");
    }

    @Scheduled(fixedDelayString = "${verivoto.access.invitation-interval:PT10S}", initialDelayString = "PT5S")
    public void run() {
        SendInvitationsUseCase.InvitationRun run = invitations.sendDue();
        if (run.delivered() > 0 || run.retried() > 0 || run.failed() > 0) {
            LOG.info("Emails to voters: {} delivered, {} to retry, {} failed", run.delivered(), run.retried(), run.failed());
        }
    }
}
