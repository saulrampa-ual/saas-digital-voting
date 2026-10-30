package com.saasvoting.ballotbox.application.service;

import com.saasvoting.ballotbox.application.port.in.MinutesUseCase;
import com.saasvoting.ballotbox.application.port.in.ViewResultsUseCase.Viewer;
import com.saasvoting.ballotbox.application.port.out.BallotRepository;
import com.saasvoting.ballotbox.application.port.out.MinutesRenderer;
import com.saasvoting.ballotbox.application.port.out.MinutesRepository;
import com.saasvoting.ballotbox.application.port.out.ResultRepository;
import com.saasvoting.ballotbox.application.port.out.UnitOfWork;
import com.saasvoting.ballotbox.domain.model.BallotNotFoundException;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.Language;
import com.saasvoting.ballotbox.domain.model.Minutes;
import com.saasvoting.ballotbox.domain.model.MinutesContent;
import com.saasvoting.ballotbox.domain.model.MinutesGenerationException;
import com.saasvoting.ballotbox.domain.model.ResultNotAvailableException;
import com.saasvoting.ballotbox.domain.model.TallyResult;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * RF-16 (D-40). The minutes of each language are generated the first time they are asked for and stored with their
 * SHA-256 (RI-10), so every later download is the same file. Only after the tally (RF-16 exception 1).
 */
public final class MinutesService implements MinutesUseCase {

    private final BallotRepository ballots;
    private final ResultRepository results;
    private final MinutesRepository minutes;
    private final MinutesRenderer renderer;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public MinutesService(BallotRepository ballots, ResultRepository results, MinutesRepository minutes,
            MinutesRenderer renderer, UnitOfWork unitOfWork, Clock clock) {
        this.ballots = Objects.requireNonNull(ballots, "ballots must not be null");
        this.results = Objects.requireNonNull(results, "results must not be null");
        this.minutes = Objects.requireNonNull(minutes, "minutes must not be null");
        this.renderer = Objects.requireNonNull(renderer, "renderer must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Minutes minutes(Viewer viewer, long processId, Language language) {
        BallotSnapshot ballot = ballots.find(processId)
                .filter(found -> found.organizationId().equals(viewer.organizationId()))
                .orElseThrow(() -> new BallotNotFoundException(processId));
        TallyResult result = results.findResult(processId)
                .orElseThrow(() -> new ResultNotAvailableException(processId));
        Optional<Minutes> stored = minutes.findMinutes(processId, language);
        if (stored.isPresent()) {
            return stored.get();
        }
        Minutes generated = generate(ballot, result, language);
        try {
            unitOfWork.inTransaction(() -> {
                minutes.saveMinutes(generated);
                return null;
            });
            return generated;
        } catch (RuntimeException e) {
            // Two first downloads at the same time: the one stored first is the minutes.
            return minutes.findMinutes(processId, language).orElseThrow(() -> new MinutesGenerationException(processId, e));
        }
    }

    private Minutes generate(BallotSnapshot ballot, TallyResult result, Language language) {
        try {
            var generatedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
            byte[] pdf = renderer.render(MinutesContent.of(ballot, result, language, generatedAt));
            return new Minutes(ballot.processId(), language, generatedAt, sha256(pdf), pdf);
        } catch (RuntimeException e) {
            throw new MinutesGenerationException(ballot.processId(), e);
        }
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
