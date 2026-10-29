package com.saasvoting.voteraccess.infrastructure.adapter.out.mail;

import com.saasvoting.voteraccess.application.port.out.Mailer;
import com.saasvoting.voteraccess.domain.model.MessageDeliveryException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

/** Plain-text email through SMTP (Mailpit in local development, D-24). Never logs the body: it carries secrets. */
public class SmtpMailer implements Mailer {

    private final JavaMailSender sender;
    private final String from;

    public SmtpMailer(JavaMailSender sender, String from) {
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
        this.from = Objects.requireNonNull(from, "from must not be null");
    }

    @Override
    public void send(MailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(message.to());
        mail.setSubject(message.subject());
        mail.setText(message.body());
        try {
            sender.send(mail);
        } catch (MailException e) {
            throw new MessageDeliveryException("Email could not be sent", e);
        }
    }
}
