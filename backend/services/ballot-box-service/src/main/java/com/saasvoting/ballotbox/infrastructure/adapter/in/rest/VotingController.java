package com.saasvoting.ballotbox.infrastructure.adapter.in.rest;

import com.saasvoting.ballotbox.application.port.in.VotingUseCase;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.InvalidSelectionException;
import com.saasvoting.ballotbox.domain.model.Selection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** RF-09 and RF-10, public endpoints of the voter PWA. The voting token is the only credential. */
@RestController
@RequestMapping("/api/v1/ballot-box/processes/{processId}")
public class VotingController {

    public record OptionResponse(long id, int position, Map<String, String> text) {
    }

    public record BallotResponse(long processId, String voteType, Map<String, String> question,
            List<OptionResponse> options, boolean blankVoteEnabled, Instant closesAt) {
    }

    public record SelectionRequest(Long optionId, Boolean blank) {
    }

    public record CastVoteRequest(@NotBlank String votingToken, @NotNull SelectionRequest selection) {
    }

    public record VoteReceiptResponse(String receiptCode) {
    }

    public record ReceiptCheckResponse(boolean present) {
    }

    private final VotingUseCase voting;

    public VotingController(VotingUseCase voting) {
        this.voting = Objects.requireNonNull(voting, "voting must not be null");
    }

    /** IMP-31: only with the voting token in {@code X-Voting-Token} (header, so it never appears in URLs or logs). */
    @GetMapping("/ballot")
    public BallotResponse ballot(@PathVariable long processId,
            @RequestHeader(name = "X-Voting-Token", required = false) String votingToken) {
        BallotSnapshot ballot = voting.ballotForVoter(processId, votingToken);
        return new BallotResponse(ballot.processId(), ballot.voteType().name(), codes(ballot.question()),
                ballot.options().stream()
                        .map(option -> new OptionResponse(option.id(), option.position(), codes(option.text())))
                        .toList(),
                ballot.blankVoteEnabled(), ballot.closesAt());
    }

    @PostMapping("/votes")
    @ResponseStatus(HttpStatus.CREATED)
    public VoteReceiptResponse castVote(@PathVariable long processId, @Valid @RequestBody CastVoteRequest request) {
        return new VoteReceiptResponse(voting.castVote(processId, request.votingToken(), selection(request.selection())));
    }

    @GetMapping("/receipts/{receiptCode}")
    public ReceiptCheckResponse receipt(@PathVariable long processId, @PathVariable String receiptCode) {
        return new ReceiptCheckResponse(voting.receiptPresent(processId, receiptCode));
    }

    /** Contract: exactly {@code {"optionId": n}} or {@code {"blank": true}}. */
    private static Selection selection(SelectionRequest request) {
        boolean blank = Boolean.TRUE.equals(request.blank());
        if (request.blank() != null && !blank) {
            throw new InvalidSelectionException();
        }
        return new Selection(request.optionId(), blank);
    }

    private static <L extends Enum<L>> Map<String, String> codes(Map<L, String> text) {
        Map<String, String> result = new LinkedHashMap<>();
        text.forEach((language, value) -> result.put(language.name().toLowerCase(Locale.ROOT), value));
        return result;
    }
}
