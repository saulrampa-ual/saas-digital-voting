package com.saasvoting.election.infrastructure.adapter.out.persistence.repository;

import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.AdminActionOutboxJpaEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AdminActionOutboxJpaRepository extends JpaRepository<AdminActionOutboxJpaEntity, Long> {

    @Query("select m from AdminActionOutboxJpaEntity m where m.nextAttemptAt <= :now order by m.id")
    List<AdminActionOutboxJpaEntity> findDue(@Param("now") Instant now, Limit limit);
}
