package com.saasvoting.election.infrastructure.config;

import com.saasvoting.election.application.port.out.CensusFreezePolicy;
import com.saasvoting.election.application.port.out.CensusRepository;
import com.saasvoting.election.application.port.out.VoterRepository;
import com.saasvoting.election.application.service.CensusService;
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
}
