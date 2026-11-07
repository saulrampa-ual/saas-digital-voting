package com.saasvoting.ballotbox.infrastructure.adapter.in.scheduling;

import com.saasvoting.ballotbox.application.port.in.RunTallyUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** RF-13 step 1: tallies the CLOSED processes. Logs only counts and process identifiers (RNF-11). */
@Component
public class TallyScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(TallyScheduler.class);

    private final RunTallyUseCase tally;

    public TallyScheduler(RunTallyUseCase tally) {
        this.tally = Objects.requireNonNull(tally, "tally must not be null");
    }

    @Scheduled(fixedDelayString = "${verivoto.tally.interval:PT10S}", initialDelayString = "PT5S")
    public void run() {
        RunTallyUseCase.TallyRun run = tally.tallyClosed();
        if (run.tallied() > 0 || run.notified() > 0) {
            LOG.info("Tally: {} process(es) tallied, {} acknowledged by election-service", run.tallied(), run.notified());
        }
        run.failures().forEach(failure -> LOG.warn("Tally pending, retried next run: {}", failure));
    }
}
