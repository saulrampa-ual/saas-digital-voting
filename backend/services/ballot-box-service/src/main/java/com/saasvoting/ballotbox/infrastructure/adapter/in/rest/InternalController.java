package com.saasvoting.ballotbox.infrastructure.adapter.in.rest;

import com.saasvoting.ballotbox.application.port.in.ReceiveBallotUseCase;
import com.saasvoting.ballotbox.domain.model.BallotOption;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.Language;
import com.saasvoting.ballotbox.domain.model.ProcessStatus;
import com.saasvoting.ballotbox.domain.model.ResultsVisibility;
import com.saasvoting.ballotbox.domain.model.VoteType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Service-to-service endpoints called by election-service (D-21, D-22): role SERVICE, never routed by the gateway. */
@RestController
@RequestMapping("/internal/v1/processes/{processId}")
public class InternalController {

    public record OptionRequest(@NotNull Long id, @NotNull @Min(1) Integer position,
            @NotEmpty Map<String, @NotBlank @Size(max = 200) String> text) {
    }

    public record BallotSnapshotRequest(
            @NotNull @Pattern(regexp = "^[A-Za-z0-9]{1,20}$") String organizationId,
            @NotNull ProcessStatus status,
            @NotNull Instant opensAt,
            @NotNull Instant closesAt,
            @NotNull VoteType voteType,
            @NotEmpty Map<String, @NotBlank @Size(max = 300) String> question,
            @NotEmpty List<@NotNull @Valid OptionRequest> options,
            @NotNull Boolean blankVoteEnabled,
            @NotNull @Min(1) Integer eligibleVoters,
            @NotNull ResultsVisibility resultsVisibility,
            UUID resultsPublicId,
            Map<String, @NotBlank @Size(max = 150) String> title) {
    }

    public record StatusUpdateRequest(@NotNull ProcessStatus status) {
    }

    private final ReceiveBallotUseCase ballots;

    public InternalController(ReceiveBallotUseCase ballots) {
        this.ballots = Objects.requireNonNull(ballots, "ballots must not be null");
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void upsertBallot(@PathVariable long processId, @Valid @RequestBody BallotSnapshotRequest request) {
        ballots.receiveBallot(new BallotSnapshot(processId, request.organizationId(), request.status(),
                request.opensAt(), request.closesAt(), request.voteType(), languages(request.question()),
                request.options().stream()
                        .map(option -> new BallotOption(option.id(), option.position(), languages(option.text())))
                        .toList(),
                request.blankVoteEnabled(), request.eligibleVoters(), request.resultsVisibility(),
                request.resultsPublicId(), null, request.title() == null ? Map.of() : languages(request.title())));
    }

    @PutMapping("/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(@PathVariable long processId, @Valid @RequestBody StatusUpdateRequest request) {
        ballots.receiveStatus(processId, request.status());
    }

    private static Map<Language, String> languages(Map<String, String> text) {
        Map<Language, String> result = new EnumMap<>(Language.class);
        text.forEach((code, value) -> result.put(Language.fromCode(code), value));
        return result;
    }
}
