package com.saasvoting.election.infrastructure.adapter.out.persistence.repository;

import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.VoterJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoterJpaRepository extends JpaRepository<VoterJpaEntity, Long> {

    @Query("select v from VoterJpaEntity v where v.id = :id and v.censusId = :censusId")
    Optional<VoterJpaEntity> findInCensus(@Param("censusId") long censusId, @Param("id") long id);

    @Query("""
            select v from VoterJpaEntity v
            where v.censusId = :censusId
              and (:search = ''
                   or lower(v.fullName) like lower(concat('%', :search, '%'))
                   or lower(v.email) like lower(concat('%', :search, '%'))
                   or lower(v.institutionalId) like lower(concat('%', :search, '%')))
            """)
    Page<VoterJpaEntity> search(@Param("censusId") long censusId, @Param("search") String search, Pageable pageable);

    @Query("select v from VoterJpaEntity v where v.censusId = :censusId and v.anonymizedAt is null order by v.id")
    List<VoterJpaEntity> findAllActiveInCensus(@Param("censusId") long censusId);

    @Query("select count(v) > 0 from VoterJpaEntity v where v.id = :id and v.anonymizedAt is not null")
    boolean isAnonymized(@Param("id") long id);

    @Query("select count(v) from VoterJpaEntity v where v.censusId = :censusId")
    long countInCensus(@Param("censusId") long censusId);

    @Query("select lower(v.email) from VoterJpaEntity v where v.censusId = :censusId")
    List<String> emails(@Param("censusId") long censusId);

    @Query("select v.institutionalId from VoterJpaEntity v where v.censusId = :censusId and v.institutionalId is not null")
    List<String> institutionalIds(@Param("censusId") long censusId);
}
