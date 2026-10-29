package com.saasvoting.election.infrastructure.adapter.out.persistence.repository;

import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.CensusJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CensusJpaRepository extends JpaRepository<CensusJpaEntity, Long> {

    @Query("select c from CensusJpaEntity c where c.id = :id and c.organizationId = :organizationId")
    Optional<CensusJpaEntity> findInOrganization(@Param("organizationId") String organizationId, @Param("id") long id);

    @Query("select c from CensusJpaEntity c where c.organizationId = :organizationId order by c.createdAt")
    List<CensusJpaEntity> findAllInOrganization(@Param("organizationId") String organizationId);
}
