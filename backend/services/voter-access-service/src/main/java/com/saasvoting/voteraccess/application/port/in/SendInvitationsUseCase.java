package com.saasvoting.voteraccess.application.port.in;

/** System task: sends the pending invitations with the personal link (RF-17 minimum needed for RF-08). */
public interface SendInvitationsUseCase {

    record InvitationRun(int delivered, int retried, int failed) {
    }

    InvitationRun sendDue();
}
