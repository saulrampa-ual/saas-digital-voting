package com.saasvoting.ballotbox.infrastructure.adapter.in.rest;

import com.saasvoting.ballotbox.application.port.in.ViewResultsUseCase;
import com.saasvoting.ballotbox.application.port.in.ViewResultsUseCase.CountedVotesView;
import com.saasvoting.ballotbox.application.port.in.ViewResultsUseCase.Fingerprints;
import com.saasvoting.ballotbox.application.port.in.ViewResultsUseCase.ResultView;
import com.saasvoting.ballotbox.application.port.in.ViewResultsUseCase.Viewer;
import com.saasvoting.ballotbox.domain.model.BallotNotFoundException;
import com.saasvoting.ballotbox.domain.model.BallotOption;
import com.saasvoting.ballotbox.domain.model.Language;
import com.saasvoting.ballotbox.domain.model.TallyResult;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * RF-12 step 6, RF-13, RF-15. Management endpoints need ADMIN or AUDITOR (export: ADMIN), checked by the security
 * filter; the organization comes from the token. Contract: {@code openapi/ballot-box-service.yaml}.
 */
@RestController
@RequestMapping("/api/v1/ballot-box")
public class ResultsController {

    public record FingerprintsResponse(long processId, String status, String finalHash, String resultHash) {
    }

    public record OptionResultResponse(long optionId, int position, Map<String, String> text, int votes,
            BigDecimal percentage) {
    }

    public record ResultResponse(long processId, String status, Instant talliedAt, String voteType,
            Map<String, String> question, List<OptionResultResponse> options, boolean blankVoteEnabled, int blankVotes,
            BigDecimal blankPercentage, int totalVotes, int eligibleVoters, BigDecimal participation, String finalHash,
            String resultHash) {
    }

    public record CountedVoteResponse(UUID voteId, Long optionId) {
    }

    public record CountedVotesResponse(long processId, String finalHash, String resultHash, int eligibleVoters,
            List<Long> optionIds, List<CountedVoteResponse> votes) {
    }

    private final ViewResultsUseCase results;

    public ResultsController(ViewResultsUseCase results) {
        this.results = Objects.requireNonNull(results, "results must not be null");
    }

    @GetMapping("/processes/{processId}/fingerprints")
    public FingerprintsResponse fingerprints(@PathVariable long processId) {
        Fingerprints fingerprints = results.fingerprints(processId);
        return new FingerprintsResponse(fingerprints.processId(), fingerprints.status().name(),
                fingerprints.finalHash(), fingerprints.resultHash());
    }

    @GetMapping("/processes/{processId}/results")
    public ResultResponse result(@AuthenticationPrincipal Jwt jwt, @PathVariable long processId) {
        return response(results.result(viewer(jwt, processId), processId));
    }

    @GetMapping("/processes/{processId}/results/counted-votes")
    public CountedVotesResponse countedVotes(@AuthenticationPrincipal Jwt jwt, @PathVariable long processId) {
        return response(results.countedVotes(viewer(jwt, processId), processId));
    }

    @GetMapping("/processes/{processId}/results/export")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal Jwt jwt, @PathVariable long processId,
            @RequestParam(defaultValue = "es") String lang) {
        Language language = "en".equalsIgnoreCase(lang) ? Language.EN : Language.ES;
        String csv = ResultCsvWriter.write(results.result(viewer(jwt, processId), processId), language);
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        byte[] file = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, file, 0, bom.length);
        System.arraycopy(body, 0, file, bom.length, body.length);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename((language == Language.EN ? "results-process-" : "resultados-proceso-") + processId + ".csv")
                        .build().toString())
                .body(file);
    }

    @GetMapping("/public/results/{publicId}")
    public ResultResponse publicResult(@PathVariable UUID publicId) {
        return response(results.publicResult(publicId));
    }

    @GetMapping("/public/results/{publicId}/counted-votes")
    public CountedVotesResponse publicCountedVotes(@PathVariable UUID publicId) {
        return response(results.publicCountedVotes(publicId));
    }

    /** A management user without organization (the super-administrator) sees no process (RNF-15). */
    private static Viewer viewer(Jwt jwt, long processId) {
        String organizationId = jwt == null ? null : jwt.getClaimAsString("organization_id");
        if (organizationId == null || organizationId.isBlank()) {
            throw new BallotNotFoundException(processId);
        }
        return new Viewer(organizationId);
    }

    private static ResultResponse response(ResultView view) {
        TallyResult result = view.result();
        List<OptionResultResponse> options = view.ballot().options().stream()
                .map(option -> optionResponse(option, result))
                .toList();
        return new ResultResponse(result.processId(), view.status().name(), result.talliedAt(),
                view.ballot().voteType().name(), codes(view.ballot().question()), options,
                view.ballot().blankVoteEnabled(), result.blankVotes(), result.percentage(result.blankVotes()),
                result.totalVotes(), result.eligibleVoters(), result.participation(), result.finalHash(),
                result.resultHash());
    }

    private static OptionResultResponse optionResponse(BallotOption option, TallyResult result) {
        int votes = result.optionVotes().getOrDefault(option.id(), 0);
        return new OptionResultResponse(option.id(), option.position(), codes(option.text()), votes,
                result.percentage(votes));
    }

    private static CountedVotesResponse response(CountedVotesView view) {
        TallyResult result = view.result();
        return new CountedVotesResponse(result.processId(), result.finalHash(), result.resultHash(),
                result.eligibleVoters(), view.optionIds(),
                view.votes().stream().map(vote -> new CountedVoteResponse(vote.voteId(), vote.optionId())).toList());
    }

    static Map<String, String> codes(Map<Language, String> text) {
        Map<String, String> result = new LinkedHashMap<>();
        text.forEach((language, value) -> result.put(language.name().toLowerCase(Locale.ROOT), value));
        return result;
    }
}
