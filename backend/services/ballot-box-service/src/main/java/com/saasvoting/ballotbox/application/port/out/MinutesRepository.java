package com.saasvoting.ballotbox.application.port.out;

import com.saasvoting.ballotbox.domain.model.Language;
import com.saasvoting.ballotbox.domain.model.Minutes;

import java.util.Optional;

/** Minutes already generated (RI-10): once stored, the same file is always served. */
public interface MinutesRepository {

    Optional<Minutes> findMinutes(long processId, Language language);

    void saveMinutes(Minutes minutes);
}
