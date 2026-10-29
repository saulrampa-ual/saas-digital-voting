package com.saasvoting.election.application.port.in;

import com.saasvoting.election.domain.model.Caller;
import com.saasvoting.election.domain.model.ImportRow;
import com.saasvoting.election.domain.model.ImportSummary;

import java.util.List;

/** RF-03: validate (dry run) or apply a CSV import already split into rows by the inbound adapter. */
public interface ImportVotersUseCase {

    ImportSummary importVoters(
            Caller caller, long censusId, List<ImportRow> rows, boolean dryRun, boolean discardInvalidLines);
}
