package com.saasvoting.ballotbox.infrastructure.adapter.out.persistence;

import com.saasvoting.ballotbox.application.port.out.BallotBoxStore;
import com.saasvoting.ballotbox.application.port.out.BallotRepository;
import com.saasvoting.ballotbox.domain.model.AuditEvent;
import com.saasvoting.ballotbox.domain.model.BallotNotFoundException;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.ChainHead;
import com.saasvoting.ballotbox.domain.model.IncidentKind;
import com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity.AuditEventJpaEntity;
import com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity.BallotSnapshotJpaEntity;
import com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity.IncidentJpaEntity;
import com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity.ReceiptJpaEntity;
import com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity.VoteJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** JPA implementation of the ballot and urn ports. */
@Repository
@Transactional
public class JpaBallotBoxStore implements BallotRepository, BallotBoxStore {

    private final EntityManager em;

    public JpaBallotBoxStore(EntityManager em) {
        this.em = Objects.requireNonNull(em, "em must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BallotSnapshot> find(long processId) {
        return Optional.ofNullable(em.find(BallotSnapshotJpaEntity.class, processId))
                .map(BallotSnapshotJpaEntity::toDomain);
    }

    @Override
    public void save(BallotSnapshot snapshot) {
        BallotSnapshotJpaEntity entity = em.find(BallotSnapshotJpaEntity.class, snapshot.processId());
        if (entity == null) {
            em.persist(BallotSnapshotJpaEntity.create(snapshot));
            return;
        }
        entity.apply(snapshot);
        entity.replaceQuestion(snapshot);
        if (!entity.sameOptionIds(snapshot)) {
            // Delete the old options before inserting the new ones (same identifiers may come back).
            entity.clearOptions();
            em.flush();
            entity.addOptions(snapshot);
        }
    }

    @Override
    public ChainHead lockChain(long processId) {
        BallotSnapshotJpaEntity entity =
                em.find(BallotSnapshotJpaEntity.class, processId, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) {
            throw new BallotNotFoundException(processId);
        }
        return entity.head();
    }

    @Override
    public void addVote(UUID voteId, long processId, Long optionId) {
        em.persist(new VoteJpaEntity(voteId, processId, optionId));
    }

    @Override
    public void addReceipt(String receiptHash, long processId) {
        em.persist(new ReceiptJpaEntity(processId, receiptHash));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean receiptExists(String receiptHash, long processId) {
        return em.find(ReceiptJpaEntity.class, new ReceiptJpaEntity.Key(processId, receiptHash)) != null;
    }

    @Override
    public void append(AuditEvent event) {
        BallotSnapshotJpaEntity entity = em.find(BallotSnapshotJpaEntity.class, event.processId());
        ChainHead head = entity.head();
        if (event.sequence() != head.sequence() + 1 || !event.previousHash().equals(head.eventHash())) {
            throw new IllegalStateException("Event does not follow the head of the chain");
        }
        em.persist(AuditEventJpaEntity.fromDomain(event));
        entity.moveHead(event.sequence(), event.eventHash());
    }

    @Override
    @Transactional(readOnly = true)
    public long countVotes(long processId) {
        return em.createQuery("select count(v) from VoteJpaEntity v where v.processId = :processId", Long.class)
                .setParameter("processId", processId)
                .getSingleResult();
    }

    @Override
    public void recordIncident(long processId, IncidentKind kind, LocalDate day) {
        em.persist(new IncidentJpaEntity(processId, kind, day));
    }
}
