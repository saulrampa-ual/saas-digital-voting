package com.saasvoting.election.infrastructure.adapter.out.persistence.repository;

import com.saasvoting.election.domain.model.ProcessStatus;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.ProcessJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProcessJpaRepository extends JpaRepository<ProcessJpaEntity, Long> {

    @Query("select p from ProcessJpaEntity p where p.id = :id and p.organizationId = :organizationId")
    Optional<ProcessJpaEntity> findInOrganization(@Param("organizationId") String organizationId, @Param("id") long id);

    @Query("select p from ProcessJpaEntity p where p.organizationId = :organizationId order by p.opensAt desc, p.id desc")
    List<ProcessJpaEntity> findAllInOrganization(@Param("organizationId") String organizationId);

    @Query("""
            select p from ProcessJpaEntity p
            where p.organizationId = :organizationId and p.status = :status
            order by p.opensAt desc, p.id desc""")
    List<ProcessJpaEntity> findAllInOrganizationWithStatus(
            @Param("organizationId") String organizationId, @Param("status") ProcessStatus status);

    /** Census freeze (D-21): is there a process of this census in one of these statuses? */
    boolean existsByCensusIdAndStatusIn(long censusId, Collection<ProcessStatus> statuses);

    /** Conditional update: only if the process is still in {@code from}. Returns the number of rows changed. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ProcessJpaEntity p set p.status = :to, p.updatedAt = :updatedAt
            where p.id = :id and p.status = :from""")
    int updateStatus(
            @Param("id") long id,
            @Param("from") ProcessStatus from,
            @Param("to") ProcessStatus to,
            @Param("updatedAt") Instant updatedAt);

    @Query("select p from ProcessJpaEntity p where p.status = :status and p.opensAt <= :now order by p.opensAt, p.id")
    List<ProcessJpaEntity> findWithStatusOpeningBefore(
            @Param("status") ProcessStatus status, @Param("now") Instant now);

    @Query("select p from ProcessJpaEntity p where p.status = :status and p.closesAt <= :now order by p.closesAt, p.id")
    List<ProcessJpaEntity> findWithStatusClosingBefore(
            @Param("status") ProcessStatus status, @Param("now") Instant now);
}
