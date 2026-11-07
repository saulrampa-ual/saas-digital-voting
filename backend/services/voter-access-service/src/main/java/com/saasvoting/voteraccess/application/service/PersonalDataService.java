package com.saasvoting.voteraccess.application.service;

import com.saasvoting.voteraccess.application.port.in.PersonalDataUseCase;
import com.saasvoting.voteraccess.application.port.out.PersonalDataRepository;
import com.saasvoting.voteraccess.application.port.out.UnitOfWork;
import com.saasvoting.voteraccess.domain.model.PersonalDataRecord;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** RF-18 (D-46). */
public final class PersonalDataService implements PersonalDataUseCase {

    private final PersonalDataRepository personalData;
    private final UnitOfWork unitOfWork;

    public PersonalDataService(PersonalDataRepository personalData, UnitOfWork unitOfWork) {
        this.personalData = Objects.requireNonNull(personalData, "personalData must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
    }

    @Override
    public List<PersonalDataRecord> records(List<RollRef> refs) {
        return refs.stream()
                .map(ref -> personalData.personalData(ref.processId(), ref.voterRef()))
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public int anonymize(long processId, List<Long> voterRefs) {
        return unitOfWork.inTransaction(() -> personalData.anonymize(processId, voterRefs));
    }
}
