package com.saasvoting.ballotbox.infrastructure.adapter.out.persistence;

import com.saasvoting.ballotbox.application.port.out.AdminActionOutbox;
import com.saasvoting.ballotbox.domain.model.AdminAction;
import com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity.AdminActionOutboxJpaEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** RF-19 (D-44): the outbox of administrative actions and the operations its dispatcher needs. */
@Component
public class JpaAdminActionOutbox implements AdminActionOutbox {

    private final EntityManager em;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public JpaAdminActionOutbox(EntityManager em, JsonMapper jsonMapper, Clock clock) {
        this.em = Objects.requireNonNull(em, "em must not be null");
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
        body.put("source", "BALLOT_BOX");
        em.persist(new AdminActionOutboxJpaEntity(action.eventId(), jsonMapper.writeValueAsString(body),
                clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<AdminActionOutboxJpaEntity> due(Instant now, int limit) {
        return em.createQuery("select m from AdminActionOutboxJpaEntity m where m.nextAttemptAt <= :now order by m.id",
                        AdminActionOutboxJpaEntity.class)
                .setParameter("now", now)
                .setMaxResults(limit)
                .getResultList();
    }

    @Transactional
    public void delivered(long id) {
        AdminActionOutboxJpaEntity message = em.find(AdminActionOutboxJpaEntity.class, id);
        if (message != null) {
            em.remove(message);
        }
    }

    @Transactional
    public void retryLater(long id, String error, Instant next) {
        AdminActionOutboxJpaEntity message = em.find(AdminActionOutboxJpaEntity.class, id);
        if (message != null) {
            message.retryLater(error, next);
        }
    }
}
