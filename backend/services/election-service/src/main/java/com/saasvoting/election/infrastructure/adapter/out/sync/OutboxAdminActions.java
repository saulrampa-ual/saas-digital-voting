package com.saasvoting.election.infrastructure.adapter.out.sync;

import com.saasvoting.election.application.port.out.AdminActionOutbox;
import com.saasvoting.election.domain.model.AdminAction;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.AdminActionOutboxJpaEntity;
import com.saasvoting.election.infrastructure.adapter.out.persistence.repository.AdminActionOutboxJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Stores each administrative action as the JSON body of {@code POST /internal/v1/admin-actions} of identity. */
@Component
public class OutboxAdminActions implements AdminActionOutbox {

    private final AdminActionOutboxJpaRepository messages;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public OutboxAdminActions(AdminActionOutboxJpaRepository messages, JsonMapper jsonMapper, Clock clock) {
        this.messages = Objects.requireNonNull(messages, "messages must not be null");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public void append(AdminAction action) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", action.eventId());
        body.put("organizationId", action.organizationId());
        body.put("occurredAt", action.occurredAt().toString());
        body.put("actorId", action.actorId());
        body.put("actorName", action.actorName());
        body.put("action", action.action());
        body.put("objectType", action.objectType());
        body.put("objectId", action.objectId());
        body.put("objectLabel", action.objectLabel());
        body.put("detail", action.detail());
        body.put("processId", action.processId());
        body.put("source", "ELECTION");
        messages.save(new AdminActionOutboxJpaEntity(action.eventId(), jsonMapper.writeValueAsString(body),
                clock.instant()));
    }
}
