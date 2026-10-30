package com.saasvoting.election.infrastructure.adapter.out.persistence.repository;

import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.ProcessSyncJpaEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProcessSyncJpaRepository extends JpaRepository<ProcessSyncJpaEntity, Long> {

    /** Pending and failed messages in creation order; failed ones block the later messages of the same target. */
    @Query("select m from ProcessSyncJpaEntity m where m.state <> :delivered order by m.id")
    List<ProcessSyncJpaEntity> findUndelivered(@Param("delivered") ProcessSyncJpaEntity.State delivered, Limit limit);

    @Query("select m from ProcessSyncJpaEntity m where m.processId = :processId order by m.id")
    List<ProcessSyncJpaEntity> findByProcess(@Param("processId") long processId);
}
