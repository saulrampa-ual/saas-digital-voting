package com.saasvoting.ballotbox.application.port.in;

import com.saasvoting.ballotbox.application.port.in.ViewResultsUseCase.Viewer;
import com.saasvoting.ballotbox.domain.model.Language;
import com.saasvoting.ballotbox.domain.model.Minutes;

/** RF-16: the administrator downloads the minutes of a tallied process. */
public interface MinutesUseCase {

    Minutes minutes(Viewer viewer, long processId, Language language);
}
