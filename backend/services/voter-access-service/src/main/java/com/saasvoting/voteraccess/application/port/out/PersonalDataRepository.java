package com.saasvoting.voteraccess.application.port.out;

import com.saasvoting.voteraccess.domain.model.PersonalDataRecord;

import java.util.List;
import java.util.Optional;

/** RF-18 (D-46): personal data of the people of a roll. */
public interface PersonalDataRepository {

    Optional<PersonalDataRecord> personalData(long processId, long voterRef);

    /**
     * Replaces name and email in the roll, removes link and code hashes, revokes live voting tokens and cancels the
     * pending emails. Participation stays (only counted). Returns how many entries changed; repeating it is harmless.
     */
    int anonymize(long processId, List<Long> voterRefs);
}
