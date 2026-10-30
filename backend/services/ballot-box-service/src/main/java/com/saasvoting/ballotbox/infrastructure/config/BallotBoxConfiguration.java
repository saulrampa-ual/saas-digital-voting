package com.saasvoting.ballotbox.infrastructure.config;

import com.saasvoting.ballotbox.application.port.out.AuditRepository;
import com.saasvoting.ballotbox.application.port.out.BallotBoxStore;
import com.saasvoting.ballotbox.application.port.out.BallotRepository;
import com.saasvoting.ballotbox.application.port.out.ElectionClient;
import com.saasvoting.ballotbox.application.port.out.MinutesRenderer;
import com.saasvoting.ballotbox.application.port.out.MinutesRepository;
import com.saasvoting.ballotbox.application.port.out.ReceiptGenerator;
import com.saasvoting.ballotbox.application.port.out.ResultRepository;
import com.saasvoting.ballotbox.application.port.out.UnitOfWork;
import com.saasvoting.ballotbox.application.port.out.VoterAccessClient;
import com.saasvoting.ballotbox.application.service.AuditService;
import com.saasvoting.ballotbox.application.service.BallotReceiverService;
import com.saasvoting.ballotbox.application.service.MinutesService;
import com.saasvoting.ballotbox.application.service.ResultQueryService;
import com.saasvoting.ballotbox.application.service.TallyService;
import com.saasvoting.ballotbox.application.service.VotingService;
import com.saasvoting.ballotbox.infrastructure.adapter.out.client.ElectionProperties;
import com.saasvoting.ballotbox.infrastructure.adapter.out.client.HttpElectionClient;
import com.saasvoting.ballotbox.infrastructure.adapter.out.client.HttpVoterAccessClient;
import com.saasvoting.ballotbox.infrastructure.adapter.out.client.KeycloakClientProperties;
import com.saasvoting.ballotbox.infrastructure.adapter.out.client.ServiceTokenProvider;
import com.saasvoting.ballotbox.infrastructure.adapter.out.client.VoterAccessProperties;
import com.saasvoting.ballotbox.infrastructure.adapter.out.pdf.PdfMinutesRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

/** Wires use cases and outbound adapters. Application services carry no Spring annotations. */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({KeycloakClientProperties.class, VoterAccessProperties.class, ElectionProperties.class})
public class BallotBoxConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    ServiceTokenProvider serviceTokenProvider(KeycloakClientProperties properties, Clock clock) {
        return new ServiceTokenProvider(properties, clock);
    }

    @Bean
    VoterAccessClient voterAccessClient(VoterAccessProperties properties, ServiceTokenProvider tokens) {
        return new HttpVoterAccessClient(properties, tokens);
    }

    @Bean
    ElectionClient electionClient(ElectionProperties properties, ServiceTokenProvider tokens) {
        return new HttpElectionClient(properties, tokens);
    }

    @Bean
    BallotReceiverService ballotReceiverService(
            BallotRepository ballots, BallotBoxStore urn, UnitOfWork unitOfWork, Clock clock) {
        return new BallotReceiverService(ballots, urn, unitOfWork, clock);
    }

    @Bean
    VotingService votingService(
            BallotRepository ballots, BallotBoxStore urn, VoterAccessClient voterAccess, ReceiptGenerator receipts,
            UnitOfWork unitOfWork, Clock clock, @Value("${verivoto.zone:Europe/Madrid}") ZoneId zone) {
        return new VotingService(ballots, urn, voterAccess, receipts, unitOfWork, clock, zone);
    }

    @Bean
    AuditService auditService(
            BallotRepository ballots, BallotBoxStore urn, AuditRepository audit, ResultRepository results,
            UnitOfWork unitOfWork, Clock clock, @Value("${verivoto.zone:Europe/Madrid}") ZoneId zone) {
        return new AuditService(ballots, urn, audit, results, unitOfWork, clock, zone);
    }

    @Bean
    TallyService tallyService(
            BallotRepository ballots, BallotBoxStore urn, ResultRepository results, ElectionClient election,
            AuditService audit, UnitOfWork unitOfWork, Clock clock,
            @Value("${verivoto.zone:Europe/Madrid}") ZoneId zone) {
        return new TallyService(ballots, urn, results, election, audit, unitOfWork, clock, zone);
    }

    @Bean
    ResultQueryService resultQueryService(BallotRepository ballots, ResultRepository results, Clock clock) {
        return new ResultQueryService(ballots, results, clock);
    }

    @Bean
    MinutesRenderer minutesRenderer(@Value("${verivoto.zone:Europe/Madrid}") ZoneId zone) {
        return new PdfMinutesRenderer(zone);
    }

    @Bean
    MinutesService minutesService(BallotRepository ballots, ResultRepository results, MinutesRepository minutes,
            MinutesRenderer renderer, UnitOfWork unitOfWork, Clock clock) {
        return new MinutesService(ballots, results, minutes, renderer, unitOfWork, clock);
    }
}
