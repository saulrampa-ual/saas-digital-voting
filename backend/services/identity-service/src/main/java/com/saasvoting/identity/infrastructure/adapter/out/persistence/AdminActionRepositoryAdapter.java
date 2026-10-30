package com.saasvoting.identity.infrastructure.adapter.out.persistence;

import com.saasvoting.identity.application.port.PageResult;
import com.saasvoting.identity.application.port.in.QueryAdminActionsUseCase.Actor;
import com.saasvoting.identity.application.port.out.AdminActionRepository;
import com.saasvoting.identity.domain.model.AdminAction;
import com.saasvoting.identity.domain.model.AdminActionFilter;
import com.saasvoting.identity.infrastructure.adapter.out.persistence.entity.AdminActionJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** JPA store of {@code admin_actions}; only inserts and reads (RNF-13). */
@Component
public class AdminActionRepositoryAdapter implements AdminActionRepository {

    private final EntityManager em;

    public AdminActionRepositoryAdapter(EntityManager em) {
        this.em = Objects.requireNonNull(em, "em must not be null");
    }

    @Override
    @Transactional
    public void appendIfAbsent(AdminAction action) {
        // The insert is skipped by the database on a duplicate event id, so concurrent deliveries are harmless.
        em.createNativeQuery("""
                insert into admin_actions (event_id, organization_id, occurred_at, actor_id, actor_name, action,
                    object_type, object_id, object_label, detail, process_id, source)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (event_id) do nothing
                """)
                .setParameter(1, action.eventId())
                .setParameter(2, action.organizationId())
                .setParameter(3, action.occurredAt())
                .setParameter(4, action.actorId())
                .setParameter(5, action.actorName())
                .setParameter(6, action.action())
                .setParameter(7, action.objectType())
                .setParameter(8, action.objectId())
                .setParameter(9, action.objectLabel())
                .setParameter(10, action.detail())
                .setParameter(11, action.processId())
                .setParameter(12, action.source())
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminAction> search(String organizationId, AdminActionFilter filter, int page, int size) {
        StringBuilder where = new StringBuilder(" where a.organizationId = :organization");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("organization", organizationId);
        if (filter.from() != null) {
            where.append(" and a.occurredAt >= :from");
            params.put("from", filter.from());
        }
        if (filter.to() != null) {
            where.append(" and a.occurredAt < :to");
            params.put("to", filter.to());
        }
        if (filter.actorId() != null) {
            where.append(" and a.actorId = :actor");
            params.put("actor", filter.actorId());
        }
        if (filter.action() != null && !filter.action().isBlank()) {
            where.append(" and a.action = :action");
            params.put("action", filter.action());
        }
        if (filter.processId() != null) {
            where.append(" and a.processId = :process");
            params.put("process", filter.processId());
        }
        TypedQuery<AdminActionJpaEntity> query = em.createQuery(
                "select a from AdminActionJpaEntity a" + where + " order by a.occurredAt desc, a.id desc",
                AdminActionJpaEntity.class);
        TypedQuery<Long> count = em.createQuery("select count(a) from AdminActionJpaEntity a" + where, Long.class);
        params.forEach((name, value) -> {
            query.setParameter(name, value);
            count.setParameter(name, value);
        });
        List<AdminAction> items = query.setFirstResult(page * size).setMaxResults(size).getResultList().stream()
                .map(AdminActionJpaEntity::toDomain)
                .toList();
        return new PageResult<>(items, page, size, count.getSingleResult());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Actor> actors(String organizationId) {
        List<Object[]> rows = em.createQuery("""
                select a.actorId, max(a.actorName) from AdminActionJpaEntity a
                where a.organizationId = :organization group by a.actorId order by max(a.actorName)
                """, Object[].class)
                .setParameter("organization", organizationId)
                .getResultList();
        return rows.stream().map(row -> new Actor((UUID) row[0], (String) row[1])).toList();
    }
}
