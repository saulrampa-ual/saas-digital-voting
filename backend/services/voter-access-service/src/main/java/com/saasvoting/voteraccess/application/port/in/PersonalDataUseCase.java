package com.saasvoting.voteraccess.application.port.in;

import com.saasvoting.voteraccess.domain.model.PersonalDataRecord;

import java.util.List;

/** RF-18 (D-46): called by election-service, which owns the census and answers the requests of the person. */
public interface PersonalDataUseCase {

    record RollRef(long processId, long voterRef) {
    }

    /** Only the entries that exist; the others are left out. */
    List<PersonalDataRecord> records(List<RollRef> refs);

    int anonymize(long processId, List<Long> voterRefs);
}
