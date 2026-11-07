package com.saasvoting.election.application.service;

import com.saasvoting.election.application.port.PageResult;
import com.saasvoting.election.application.port.in.CensusView;
import com.saasvoting.election.application.port.in.ImportVotersUseCase;
import com.saasvoting.election.application.port.in.ManageCensusesUseCase;
import com.saasvoting.election.application.port.in.ManageVotersUseCase;
import com.saasvoting.election.application.port.out.CensusFreezePolicy;
import com.saasvoting.election.application.port.out.CensusRepository;
import com.saasvoting.election.application.port.out.VoterRepository;
import com.saasvoting.election.domain.model.Caller;
import com.saasvoting.election.domain.model.Census;
import com.saasvoting.election.domain.model.CensusFrozenException;
import com.saasvoting.election.domain.model.CensusNotFoundException;
import com.saasvoting.election.domain.model.DuplicateReason;
import com.saasvoting.election.domain.model.DuplicateVoterException;
import com.saasvoting.election.domain.model.ImportRow;
import com.saasvoting.election.domain.model.ImportSummary;
import com.saasvoting.election.domain.model.InvalidVoterDataException;
import com.saasvoting.election.domain.model.Language;
import com.saasvoting.election.domain.model.OperationNotAllowedException;
import com.saasvoting.election.domain.model.Voter;
import com.saasvoting.election.domain.model.VoterData;
import com.saasvoting.election.domain.model.VoterAnonymizedException;
import com.saasvoting.election.domain.model.VoterNotFoundException;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Census management (RF-03). */
public final class CensusService implements ManageCensusesUseCase, ManageVotersUseCase, ImportVotersUseCase {

    private final CensusRepository censuses;
    private final VoterRepository voters;
    private final CensusFreezePolicy freezePolicy;
    private final Clock clock;

    public CensusService(
            CensusRepository censuses, VoterRepository voters, CensusFreezePolicy freezePolicy, Clock clock) {
        this.censuses = Objects.requireNonNull(censuses, "censuses must not be null");
        this.voters = Objects.requireNonNull(voters, "voters must not be null");
        this.freezePolicy = Objects.requireNonNull(freezePolicy, "freezePolicy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public List<CensusView> list(Caller caller) {
        requireReader(caller);
        return censuses.findByOrganization(caller.requireOrganization()).stream().map(this::view).toList();
    }

    @Override
    public CensusView create(Caller caller, String name) {
        requireEditor(caller);
        return view(censuses.save(Census.create(caller.requireOrganization(), name, clock.instant())));
    }

    @Override
    public CensusView get(Caller caller, long censusId) {
        requireReader(caller);
        return view(census(caller, censusId));
    }

    @Override
    public PageResult<Voter> list(Caller caller, long censusId, String search, int page, int size) {
        requireReader(caller);
        census(caller, censusId);
        return voters.search(censusId, search == null ? "" : search.strip(), page, size);
    }

    @Override
    public Voter add(Caller caller, long censusId, VoterData data) {
        requireEditor(caller);
        Census census = editableCensus(caller, censusId);
        rejectDuplicate(censusId, data, null);
        return voters.save(Voter.create(census, data, clock.instant()));
    }

    @Override
    public Voter update(Caller caller, long censusId, long voterId, VoterData data) {
        requireEditor(caller);
        editableCensus(caller, censusId);
        Voter voter = voters.findById(censusId, voterId).orElseThrow(() -> new VoterNotFoundException(voterId));
        if (voters.isAnonymized(voterId)) {
            throw new VoterAnonymizedException(voterId);
        }
        rejectDuplicate(censusId, data, voter.data());
        return voters.save(voter.withData(data));
    }

    @Override
    public void remove(Caller caller, long censusId, long voterId) {
        requireEditor(caller);
        editableCensus(caller, censusId);
        Voter voter = voters.findById(censusId, voterId).orElseThrow(() -> new VoterNotFoundException(voterId));
        voters.delete(voter);
    }

    /**
     * Validates every row, detects duplicates against the census and inside the file, and stores the new voters
     * only when {@code dryRun} is false and either there are no invalid lines or the caller chose to discard them.
     */
    @Override
    public ImportSummary importVoters(
            Caller caller, long censusId, List<ImportRow> rows, boolean dryRun, boolean discardInvalidLines) {
        requireEditor(caller);
        Census census = editableCensus(caller, censusId);

        Set<String> emails = new HashSet<>(voters.emails(censusId));
        Set<String> institutionalIds = new HashSet<>(voters.institutionalIds(censusId));
        List<Voter> toCreate = new ArrayList<>();
        List<ImportSummary.Duplicate> duplicates = new ArrayList<>();
        List<ImportSummary.InvalidLine> invalidLines = new ArrayList<>();

        for (ImportRow row : rows) {
            VoterData data;
            try {
                data = toVoterData(row);
            } catch (InvalidVoterDataException e) {
                invalidLines.add(new ImportSummary.InvalidLine(row.line(), e.userMessage()));
                continue;
            }
            if (emails.contains(data.email())) {
                duplicates.add(new ImportSummary.Duplicate(row.line(), DuplicateReason.EMAIL));
                continue;
            }
            if (data.institutionalId() != null && institutionalIds.contains(data.institutionalId())) {
                duplicates.add(new ImportSummary.Duplicate(row.line(), DuplicateReason.INSTITUTIONAL_ID));
                continue;
            }
            emails.add(data.email());
            if (data.institutionalId() != null) {
                institutionalIds.add(data.institutionalId());
            }
            toCreate.add(Voter.create(census, data, clock.instant()));
        }

        boolean apply = !dryRun && (invalidLines.isEmpty() || discardInvalidLines);
        if (apply && !toCreate.isEmpty()) {
            voters.saveAll(toCreate);
        }
        return new ImportSummary(apply, toCreate.size(), duplicates, invalidLines);
    }

    private static VoterData toVoterData(ImportRow row) {
        Language language;
        try {
            language = row.language() == null || row.language().isBlank() ? null : Language.fromCode(row.language());
        } catch (IllegalArgumentException e) {
            throw new InvalidVoterDataException("language", "El idioma debe ser «es» o «en».");
        }
        return new VoterData(
                row.fullName() == null ? "" : row.fullName(),
                row.email() == null ? "" : row.email(),
                row.phone(),
                row.institutionalId(),
                language);
    }

    private void rejectDuplicate(long censusId, VoterData data, VoterData current) {
        boolean emailChanged = current == null || !current.email().equals(data.email());
        if (emailChanged && voters.emails(censusId).contains(data.email())) {
            throw new DuplicateVoterException(DuplicateReason.EMAIL);
        }
        boolean institutionalIdChanged = current == null
                || !Objects.equals(current.institutionalId(), data.institutionalId());
        if (data.institutionalId() != null && institutionalIdChanged
                && voters.institutionalIds(censusId).contains(data.institutionalId())) {
            throw new DuplicateVoterException(DuplicateReason.INSTITUTIONAL_ID);
        }
    }

    private Census census(Caller caller, long censusId) {
        return censuses.findById(caller.requireOrganization(), censusId)
                .orElseThrow(() -> new CensusNotFoundException(censusId));
    }

    private Census editableCensus(Caller caller, long censusId) {
        Census census = census(caller, censusId);
        if (freezePolicy.isFrozen(censusId)) {
            throw new CensusFrozenException(censusId);
        }
        return census;
    }

    private CensusView view(Census census) {
        return new CensusView(census, censuses.countVoters(census.id()), freezePolicy.isFrozen(census.id()));
    }

    private static void requireReader(Caller caller) {
        if (!caller.canReadCensus()) {
            throw new OperationNotAllowedException("read census");
        }
    }

    private static void requireEditor(Caller caller) {
        if (!caller.canEditCensus()) {
            throw new OperationNotAllowedException("edit census");
        }
    }
}
