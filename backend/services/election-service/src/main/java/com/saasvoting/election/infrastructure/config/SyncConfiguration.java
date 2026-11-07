package com.saasvoting.election.infrastructure.config;

import com.saasvoting.election.infrastructure.adapter.out.persistence.repository.ProcessSyncJpaRepository;
import com.saasvoting.election.infrastructure.adapter.out.persistence.repository.AdminActionOutboxJpaRepository;
import com.saasvoting.election.infrastructure.adapter.out.sync.AdminActionDispatcher;
import com.saasvoting.election.infrastructure.adapter.out.sync.HttpVoterAccessRecords;
import com.saasvoting.election.infrastructure.adapter.out.sync.ProcessSyncDispatcher;
import com.saasvoting.election.infrastructure.adapter.out.sync.SyncComponents;
import com.saasvoting.election.infrastructure.adapter.out.sync.SyncProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.time.Clock;

/** Scheduled tasks (lifecycle and outbox delivery) and the HTTP clients of the outbox (D-21, D-22). */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(SyncProperties.class)
public class SyncConfiguration {

    @Bean
    ProcessSyncDispatcher processSyncDispatcher(
            ProcessSyncJpaRepository messages, SyncProperties properties, Clock clock) {
        return SyncComponents.dispatcher(messages, RestClient.builder(), properties, clock);
    }

    @Bean
    HttpVoterAccessRecords voterAccessRecords(SyncProperties properties, Clock clock) {
        return SyncComponents.voterAccessRecords(RestClient.builder(), properties, clock);
    }

    @Bean
    AdminActionDispatcher adminActionDispatcher(
            AdminActionOutboxJpaRepository messages, SyncProperties properties, Clock clock) {
        return SyncComponents.adminActionDispatcher(messages, RestClient.builder(), properties, clock);
    }
}
