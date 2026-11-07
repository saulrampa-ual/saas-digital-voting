package com.saasvoting.voteraccess.application.port.out;

/** Sends plain-text emails to voters (invitation and access code). Throws {@code MessageDeliveryException}. */
public interface Mailer {

    record MailMessage(String to, String subject, String body) {
    }

    void send(MailMessage message);
}
