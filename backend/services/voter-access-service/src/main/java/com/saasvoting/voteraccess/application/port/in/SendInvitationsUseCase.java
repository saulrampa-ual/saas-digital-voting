package com.saasvoting.voteraccess.application.port.in;

/** System task: sends the pending emails to voters: invitations, reminders and closing notices (RF-17). */
public interface SendInvitationsUseCase {

    record InvitationRun(int delivered, int retried, int failed) {
    }

    InvitationRun sendDue();
}
