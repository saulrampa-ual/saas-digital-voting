package com.saasvoting.election.infrastructure.adapter.in.scheduling;

import com.saasvoting.election.application.port.in.RunLifecycleUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * RF-07, steps 4-5: opens and closes the processes whose time has arrived. Runs every
 * {@code verivoto.lifecycle.interval} (30 s by default), so a process opens or closes at most that much later than
 * its configured time; ballot-box rejects votes after {@code closesAt} anyway (ADR-0002).
 */
@Component
public class LifecycleScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(LifecycleScheduler.class);

    private final RunLifecycleUseCase lifecycle;

    public LifecycleScheduler(RunLifecycleUseCase lifecycle) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle must not be null");
    }

    @Scheduled(fixedDelayString = "${verivoto.lifecycle.interval:PT30S}", initialDelayString = "PT5S")
    public void run() {
        RunLifecycleUseCase.LifecycleRun run = lifecycle.openAndCloseDue();
        if (run.opened() > 0 || run.closed() > 0) {
            LOG.info("Lifecycle: {} process(es) opened, {} closed", run.opened(), run.closed());
        }
        run.failures().forEach(failure -> LOG.warn("Lifecycle transition failed, retried next run: {}", failure));
    }
}
