package com.saasvoting.election.application.service;

import com.saasvoting.election.application.port.in.ManagePrivacyUseCase;
import com.saasvoting.election.application.port.out.PrivacyRepository;
import com.saasvoting.election.application.port.out.ProcessSyncOutbox;
import com.saasvoting.election.application.port.out.UnitOfWork;
import com.saasvoting.election.application.port.out.VoterAccessRecords;
import com.saasvoting.election.domain.model.Caller;
import com.saasvoting.election.domain.model.ErasureBlockedException;
import com.saasvoting.election.domain.model.ErasureResult;
import com.saasvoting.election.domain.model.OperationNotAllowedException;
import com.saasvoting.election.domain.model.PersonalRecord;
import com.saasvoting.election.domain.model.PrivacyExport;
import com.saasvoting.election.domain.model.PrivacyQuery;
import com.saasvoting.election.domain.model.PrivacySubjectNotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RF-18 (D-46). The vote is never part of it: it is not linked to the person (RNF-11). An entry of a census that a
 * process used is anonymized, not deleted, so results, participation and the audit chain stay as they were; the roll
 * of voter-access is anonymized through the outbox, which retries until it is done.
 */
public final class PrivacyService implements ManagePrivacyUseCase {

    private final PrivacyRepository privacy;
    private final VoterAccessRecords voterAccess;
    private final ProcessSyncOutbox outbox;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public PrivacyService(PrivacyRepository privacy, VoterAccessRecords voterAccess, ProcessSyncOutbox outbox,
            UnitOfWork unitOfWork, Clock clock) {
        this.privacy = Objects.requireNonNull(privacy, "privacy must not be null");
        this.voterAccess = Objects.requireNonNull(voterAccess, "voterAccess must not be null");
        this.outbox = Objects.requireNonNull(outbox, "outbox must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public List<PersonalRecord> find(Caller caller, String query) {
        return records(caller, PrivacyQuery.parse(query));
    }

    @Override
    public PrivacyExport export(Caller caller, String query) {
        List<PersonalRecord> records = records(caller, PrivacyQuery.parse(query));
        List<VoterAccessRecords.RollRef> refs = new ArrayList<>();
        for (PersonalRecord record : records) {
            record.processes().forEach(process -> refs.add(new VoterAccessRecords.RollRef(process.id(), record.voterId())));
        }
        return new PrivacyExport(records, refs.isEmpty() ? List.of() : voterAccess.records(refs));
    }

    @Override
    public ErasureResult erase(Caller caller, String query) {
        PrivacyQuery parsed = PrivacyQuery.parse(query);
        return unitOfWork.inTransaction(() -> {
            List<PersonalRecord> records = records(caller, parsed);
            List<Long> blocking = records.stream()
                    .flatMap(record -> record.activeProcesses().stream())
                    .map(PersonalRecord.ProcessRef::id)
                    .distinct()
                    .toList();
            if (!blocking.isEmpty()) {
                throw new ErasureBlockedException(blocking);
            }
            Instant now = clock.instant();
            List<Long> delete = new ArrayList<>();
            List<Long> anonymize = new ArrayList<>();
            Map<Long, List<Long>> rollByProcess = new LinkedHashMap<>();
            for (PersonalRecord record : records) {
                if (record.mustBeAnonymized()) {
                    anonymize.add(record.voterId());
                    record.processes().forEach(process ->
                            rollByProcess.computeIfAbsent(process.id(), id -> new ArrayList<>()).add(record.voterId()));
                } else {
                    delete.add(record.voterId());
                }
            }
            if (!delete.isEmpty()) {
                privacy.delete(delete);
            }
            if (!anonymize.isEmpty()) {
                privacy.anonymize(anonymize, now);
            }
            rollByProcess.forEach(outbox::rollAnonymized);
            return new ErasureResult(delete.size(), anonymize.size(), records.getFirst().voterId());
        });
    }

    private List<PersonalRecord> records(Caller caller, PrivacyQuery query) {
        if (!caller.hasRole(Caller.ADMIN)) {
            throw new OperationNotAllowedException("privacy");
        }
        List<PersonalRecord> records = privacy.findRecords(caller.requireOrganization(), query);
        if (records.isEmpty()) {
            throw new PrivacySubjectNotFoundException();
        }
        return records;
    }
}
