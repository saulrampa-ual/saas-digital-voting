package com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.voteraccess.domain.model.Participation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/** RI-7 with deviation D-18: only the day of the vote. */
@Entity
@Table(name = "participation")
public class ParticipationJpaEntity {

    @Id
    @Column(name = "roll_entry_id", nullable = false, updatable = false)
    private UUID rollEntryId;

    @Column(name = "has_voted", nullable = false)
    private boolean hasVoted;

    @Column(name = "voted_on")
    private LocalDate votedOn;

    protected ParticipationJpaEntity() {
        // Required by JPA.
    }

    public static ParticipationJpaEntity create(Participation participation) {
        ParticipationJpaEntity entity = new ParticipationJpaEntity();
        entity.rollEntryId = participation.rollEntryId();
        entity.apply(participation);
        return entity;
    }

    public void apply(Participation participation) {
        hasVoted = participation.hasVoted();
        votedOn = participation.votedOn();
    }

    public Participation toDomain() {
        return new Participation(rollEntryId, hasVoted, votedOn);
    }
}
