package com.saasvoting.voteraccess.infrastructure.config;

import com.saasvoting.voteraccess.application.port.out.AdminActionOutbox;
import com.saasvoting.voteraccess.application.port.out.Mailer;
import com.saasvoting.voteraccess.application.port.out.NotificationRepository;
import com.saasvoting.voteraccess.application.port.out.ParticipationRepository;
import com.saasvoting.voteraccess.application.port.out.ReminderRepository;
import com.saasvoting.voteraccess.application.port.out.ResultsTokenRepository;
import com.saasvoting.voteraccess.application.port.out.RollRepository;
import com.saasvoting.voteraccess.application.port.out.SecretGenerator;
import com.saasvoting.voteraccess.application.port.out.SnapshotRepository;
import com.saasvoting.voteraccess.application.port.out.UnitOfWork;
import com.saasvoting.voteraccess.application.port.out.VotingTokenRepository;
import com.saasvoting.voteraccess.application.service.AdminActionService;
import com.saasvoting.voteraccess.application.service.InvitationService;
import com.saasvoting.voteraccess.application.service.NotificationAdminService;
import com.saasvoting.voteraccess.application.service.ParticipationService;
import com.saasvoting.voteraccess.application.service.ProcessReceiverService;
import com.saasvoting.voteraccess.application.service.ResultsAccessService;
import com.saasvoting.voteraccess.application.service.VoterAccessService;
import com.saasvoting.voteraccess.application.service.VoterMessages;
import com.saasvoting.voteraccess.application.service.VotingTokenService;
import com.saasvoting.voteraccess.infrastructure.adapter.out.client.AdminActionDispatcher;
import com.saasvoting.voteraccess.infrastructure.adapter.out.client.IdentityProperties;
import com.saasvoting.voteraccess.infrastructure.adapter.out.client.KeycloakClientProperties;
import com.saasvoting.voteraccess.infrastructure.adapter.out.client.ServiceTokenProvider;
import com.saasvoting.voteraccess.infrastructure.adapter.out.mail.SmtpMailer;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.JpaAdminActionOutbox;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/** Wires use cases and outbound adapters. Application services carry no Spring annotations. */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({AccessProperties.class, KeycloakClientProperties.class, IdentityProperties.class})
public class VoterAccessConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    Mailer mailer(JavaMailSender sender, AccessProperties properties) {
        return new SmtpMailer(sender, properties.mailFrom());
    }

    @Bean
    VoterMessages voterMessages(AccessProperties properties) {
        return new VoterMessages(properties.publicBaseUrl(), properties.zone());
    }

    @Bean
    ParticipationService participationService(
            SnapshotRepository snapshots, ParticipationRepository participation, Clock clock) {
        return new ParticipationService(snapshots, participation, clock);
    }

    @Bean
    ProcessReceiverService processReceiverService(
            SnapshotRepository snapshots, RollRepository roll, NotificationRepository notifications,
            ReminderRepository reminders, UnitOfWork unitOfWork, Clock clock) {
        return new ProcessReceiverService(snapshots, roll, notifications, reminders, unitOfWork, clock);
    }

    @Bean
    NotificationAdminService notificationAdminService(
            SnapshotRepository snapshots, NotificationRepository notifications, ReminderRepository reminders,
            UnitOfWork unitOfWork, Clock clock) {
        return new NotificationAdminService(snapshots, notifications, reminders, unitOfWork, clock);
    }

    @Bean
    ServiceTokenProvider serviceTokenProvider(KeycloakClientProperties properties, Clock clock) {
        return new ServiceTokenProvider(properties, clock);
    }

    @Bean
    AdminActionService adminActionService(AdminActionOutbox outbox) {
        return new AdminActionService(outbox);
    }

    @Bean
    AdminActionDispatcher adminActionDispatcher(JpaAdminActionOutbox outbox, IdentityProperties properties,
            ServiceTokenProvider tokens, Clock clock) {
        return new AdminActionDispatcher(outbox, properties, tokens, clock);
    }

    @Bean
    VoterAccessService voterAccessService(
            SnapshotRepository snapshots, RollRepository roll, VotingTokenRepository tokens,
            ResultsTokenRepository resultsTokens, SecretGenerator secrets, Mailer mailer, VoterMessages messages,
            AccessProperties properties, UnitOfWork unitOfWork, Clock clock) {
        return new VoterAccessService(snapshots, roll, tokens, resultsTokens, secrets, mailer, messages,
                properties.policy(), unitOfWork, clock);
    }

    @Bean
    ResultsAccessService resultsAccessService(
            ResultsTokenRepository resultsTokens, SnapshotRepository snapshots, SecretGenerator secrets,
            UnitOfWork unitOfWork, Clock clock) {
        return new ResultsAccessService(resultsTokens, snapshots, secrets, unitOfWork, clock);
    }

    @Bean
    VotingTokenService votingTokenService(
            VotingTokenRepository tokens, RollRepository roll, SnapshotRepository snapshots, SecretGenerator secrets,
            UnitOfWork unitOfWork, Clock clock, AccessProperties properties) {
        return new VotingTokenService(tokens, roll, snapshots, secrets, unitOfWork, clock, properties.zone());
    }

    @Bean
    InvitationService invitationService(
            NotificationRepository notifications, RollRepository roll, SnapshotRepository snapshots,
            SecretGenerator secrets, Mailer mailer, VoterMessages messages, AccessProperties properties,
            UnitOfWork unitOfWork, Clock clock) {
        return new InvitationService(notifications, roll, snapshots, secrets, mailer, messages, properties.policy(),
                unitOfWork, clock);
    }
}
