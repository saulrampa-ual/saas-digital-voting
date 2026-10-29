package com.saasvoting.election.infrastructure.adapter.out.sync;

import com.saasvoting.election.application.port.out.ProcessSyncOutbox;
import com.saasvoting.election.domain.model.Voter;
import com.saasvoting.election.domain.model.VotingProcess;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.ProcessSyncJpaEntity;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.ProcessSyncJpaEntity.MessageType;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.ProcessSyncJpaEntity.Target;
import com.saasvoting.election.infrastructure.adapter.out.persistence.repository.ProcessSyncJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

/** Writes the outbox messages; must run inside the transaction of the status change (ADR-0002). */
@Component
public class OutboxProcessSync implements ProcessSyncOutbox {

    private final ProcessSyncJpaRepository messages;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public OutboxProcessSync(ProcessSyncJpaRepository messages, JsonMapper jsonMapper, Clock clock) {
        this.messages = Objects.requireNonNull(messages, "messages must not be null");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void processScheduled(VotingProcess process, List<Voter> roll) {
        enqueue(process, Target.VOTER_ACCESS, MessageType.SNAPSHOT, SyncMessages.processSnapshot(process, roll));
        enqueue(process, Target.BALLOT_BOX, MessageType.SNAPSHOT, SyncMessages.ballotSnapshot(process, roll.size()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void statusChanged(VotingProcess process) {
        SyncMessages.StatusUpdate update = new SyncMessages.StatusUpdate(process.status());
        enqueue(process, Target.VOTER_ACCESS, MessageType.STATUS, update);
        enqueue(process, Target.BALLOT_BOX, MessageType.STATUS, update);
    }

    private void enqueue(VotingProcess process, Target target, MessageType type, Object body) {
        messages.save(ProcessSyncJpaEntity.pending(
                process.id(), target, type, jsonMapper.writeValueAsString(body), clock.instant()));
    }
}
