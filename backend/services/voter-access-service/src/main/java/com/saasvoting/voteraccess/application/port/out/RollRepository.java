package com.saasvoting.voteraccess.application.port.out;

import com.saasvoting.voteraccess.domain.model.AccessCredential;
import com.saasvoting.voteraccess.domain.model.Notification;
import com.saasvoting.voteraccess.domain.model.Participation;
import com.saasvoting.voteraccess.domain.model.RollEntry;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Frozen census of each process with the access credential and participation of every voter. */
public interface RollRepository {

    /** Voter references already in the roll of the process. */
    Set<Long> voterRefs(long processId);

    /** Stores new roll entries with their (empty) credentials, participation and pending invitation in bulk. */
    void addAll(List<RollEntry> entries, List<AccessCredential> credentials, List<Participation> participations,
            List<Notification> invitations);

    Optional<RollEntry> findEntry(UUID rollEntryId);

    Optional<AccessCredential> findCredentialByLinkHash(String linkHash);

    /** Reads and locks the credential until the end of the transaction (concurrent attempts, RNF-14). */
    AccessCredential lockCredential(UUID rollEntryId);

    void save(AccessCredential credential);

    /** Reads and locks the participation until the end of the transaction (single vote, RNF-12). */
    Participation lockParticipation(UUID rollEntryId);

    Participation findParticipation(UUID rollEntryId);

    void save(Participation participation);
}
