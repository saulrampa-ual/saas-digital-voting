package com.saasvoting.voteraccess.infrastructure.adapter.in.rest;

import com.saasvoting.voteraccess.application.port.in.ReceiveProcessUseCase;
import com.saasvoting.voteraccess.application.port.in.ResultsAccessUseCase;
import com.saasvoting.voteraccess.application.port.in.ViewParticipationUseCase;
import com.saasvoting.voteraccess.application.port.in.VotingTokenUseCase;
import com.saasvoting.voteraccess.domain.model.Language;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.ProcessStatus;
import com.saasvoting.voteraccess.domain.model.ResultsVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

/**
 * Service-to-service endpoints (D-22): only tokens with role SERVICE; never routed by the gateway.
 * Called by election-service (process and status) and ballot-box (voting tokens, results tokens and the number of
 * people who voted). Each route accepts only the client that uses it (IMP-26, {@code SecurityConfiguration}).
 */
@RestController
@RequestMapping("/internal/v1")
public class InternalController {

    public record RollEntryRequest(
            @NotNull Long voterRef,
            @NotBlank @Size(max = 60) String displayName,
            @NotBlank @Email @Size(max = 100) String email,
            @NotNull @Pattern(regexp = "es|en") String language) {
    }

    public record ProcessSnapshotRequest(
            @NotNull @Pattern(regexp = "^[A-Za-z0-9]{1,20}$") String organizationId,
            @NotEmpty Map<String, @NotBlank @Size(max = 150) String> title,
            @NotNull ProcessStatus status,
            @NotNull Instant opensAt,
            @NotNull Instant closesAt,
            @NotNull @Pattern(regexp = "es|en") String defaultLanguage,
            @NotNull ResultsVisibility resultsVisibility,
            @NotNull List<@NotNull @Valid RollEntryRequest> roll) {
    }

    public record StatusUpdateRequest(@NotNull ProcessStatus status) {
    }

    public record TokenOperationRequest(@NotBlank String votingToken, @NotNull Long processId) {
    }

    public record ResultsTokenCheckRequest(@NotBlank String resultsToken, @NotNull Long processId) {
    }

    private final ReceiveProcessUseCase processes;
    private final VotingTokenUseCase tokens;
    private final ResultsAccessUseCase results;
    private final ViewParticipationUseCase participation;

    public record VotedCountResponse(long processId, int voted) {
    }

    public InternalController(ReceiveProcessUseCase processes, VotingTokenUseCase tokens,
            ResultsAccessUseCase results, ViewParticipationUseCase participation) {
        this.processes = Objects.requireNonNull(processes, "processes must not be null");
        this.tokens = Objects.requireNonNull(tokens, "tokens must not be null");
        this.results = Objects.requireNonNull(results, "results must not be null");
        this.participation = Objects.requireNonNull(participation, "participation must not be null");
    }

    @PutMapping("/processes/{processId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void upsertSnapshot(@PathVariable long processId, @Valid @RequestBody ProcessSnapshotRequest request) {
        Map<Language, String> title = new EnumMap<>(Language.class);
        request.title().forEach((code, text) -> title.put(Language.fromCode(code), text));
        ProcessSnapshot snapshot = new ProcessSnapshot(processId, request.organizationId(), title, request.status(),
                request.opensAt(), request.closesAt(), Language.fromCode(request.defaultLanguage()),
                request.resultsVisibility());
        processes.receiveSnapshot(snapshot, request.roll().stream()
                .map(entry -> new ReceiveProcessUseCase.RollData(entry.voterRef(), entry.displayName(),
                        entry.email(), Language.fromCode(entry.language())))
                .toList());
    }

    @PutMapping("/processes/{processId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(@PathVariable long processId, @Valid @RequestBody StatusUpdateRequest request) {
        processes.receiveStatus(processId, request.status());
    }

    @PostMapping("/voting-tokens/consume")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void consume(@Valid @RequestBody TokenOperationRequest request) {
        tokens.consume(request.votingToken(), request.processId());
    }

    @PostMapping("/voting-tokens/revert")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revert(@Valid @RequestBody TokenOperationRequest request) {
        tokens.revert(request.votingToken(), request.processId());
    }

    /** D-41: 204 if the results token is valid for the process; 404 unknown, 410 expired. */
    @PostMapping("/results-tokens/check")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void checkResultsToken(@Valid @RequestBody ResultsTokenCheckRequest request) {
        results.check(request.resultsToken(), request.processId());
    }

    /** IMP-31: the voting token is valid right now; nothing is changed. 404, 409, 410 or 422 like {@code consume}. */
    @PostMapping("/voting-tokens/check")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void checkVotingToken(@Valid @RequestBody TokenOperationRequest request) {
        tokens.check(request.votingToken(), request.processId());
    }

    /** IMP-28: number of people who voted, compared by ballot-box with the urn at the tally. */
    @GetMapping("/processes/{processId}/voted-count")
    public VotedCountResponse votedCount(@PathVariable long processId) {
        return new VotedCountResponse(processId, participation.votedCount(processId));
    }
}
