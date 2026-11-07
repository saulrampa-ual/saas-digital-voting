package com.saasvoting.election.infrastructure.adapter.out.persistence.repository;

import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.ProcessStatusChangeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProcessStatusChangeJpaRepository extends JpaRepository<ProcessStatusChangeJpaEntity, Long> {

    @Query("""
            select c from ProcessStatusChangeJpaEntity c
            where c.processId = :processId
            order by c.changedAt, c.id""")
    List<ProcessStatusChangeJpaEntity> findByProcess(@Param("processId") long processId);
}
