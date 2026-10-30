package com.saasvoting.election.infrastructure.config;

import com.saasvoting.election.application.port.out.CensusFreezePolicy;
import com.saasvoting.election.application.port.out.CensusRepository;
import com.saasvoting.election.application.port.out.ProcessRepository;
import com.saasvoting.election.application.port.out.ProcessSyncOutbox;
import com.saasvoting.election.application.port.out.UnitOfWork;
import com.saasvoting.election.application.port.out.VoterRepository;
import com.saasvoting.election.application.port.out.AdminActionOutbox;
import com.saasvoting.election.application.service.AdminActionService;
import com.saasvoting.election.application.service.CensusService;
import com.saasvoting.election.application.service.LifecycleService;
import com.saasvoting.election.application.service.ProcessService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Wires use cases and outbound adapters. Application services carry no Spring annotations. */
@Configuration
public class ElectionServiceConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    CensusService censusService(
            CensusRepository censuses, VoterRepository voters, CensusFreezePolicy freezePolicy, Clock clock) {
        return new CensusService(censuses, voters, freezePolicy, clock);
    }

    @Bean
    AdminActionService adminActionService(AdminActionOutbox outbox) {
        return new AdminActionService(outbox);
    }

    @Bean
    ProcessService processService(ProcessRepository processes, CensusRepository censuses, Clock clock) {
        return new ProcessService(processes, censuses, clock);
    }

    @Bean
    LifecycleService lifecycleService(
            ProcessRepository processes, VoterRepository voters, ProcessSyncOutbox outbox, UnitOfWork unitOfWork,
            Clock clock) {
        return new LifecycleService(processes, voters, outbox, unitOfWork, clock);
    }
}
