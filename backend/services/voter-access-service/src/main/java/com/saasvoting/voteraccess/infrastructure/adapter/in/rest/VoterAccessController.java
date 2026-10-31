package com.saasvoting.voteraccess.infrastructure.adapter.in.rest;

import com.saasvoting.voteraccess.application.port.in.VoterAccessUseCase;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** RF-08, public endpoints of the voter PWA (no account: the link and the code identify the voter). */
@RestController
@RequestMapping("/api/v1/voter-access/links/{linkToken}")
public class VoterAccessController {

    public record LinkStatusResponse(long processId, Map<String, String> title, String status, Instant opensAt,
            Instant closesAt, String defaultLanguage, String authentication) {
    }

    public record InstitutionalAccountRequest(@NotNull String idToken) {
    }

    public record CodeSentResponse(String maskedEmail, Instant expiresAt, Instant resendAvailableAt) {
    }

    public record VerifyRequest(@NotNull String code) {
    }

    public record AccessGrantedResponse(boolean hasVoted, LocalDate votedOn, String votingToken,
            Instant votingTokenExpiresAt, String resultsToken, Instant resultsTokenExpiresAt) {
    }

    private final VoterAccessUseCase access;

    public VoterAccessController(VoterAccessUseCase access) {
        this.access = Objects.requireNonNull(access, "access must not be null");
    }

    @GetMapping
    public LinkStatusResponse linkStatus(@PathVariable String linkToken) {
        ProcessSnapshot process = access.linkStatus(linkToken);
        Map<String, String> title = new LinkedHashMap<>();
        process.title().forEach((language, text) -> title.put(language.code(), text));
        return new LinkStatusResponse(process.processId(), title, process.status().name(), process.opensAt(),
                process.closesAt(), process.defaultLanguage().code(), process.authentication().name());
    }

    @PostMapping("/otp")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CodeSentResponse sendCode(@PathVariable String linkToken) {
        VoterAccessUseCase.CodeSent sent = access.sendCode(linkToken);
        return new CodeSentResponse(sent.maskedEmail(), sent.expiresAt(), sent.resendAvailableAt());
    }

    @PostMapping("/otp/verify")
    public AccessGrantedResponse verify(@PathVariable String linkToken, @Valid @RequestBody VerifyRequest request) {
        VoterAccessUseCase.AccessGranted granted = access.verifyCode(linkToken, request.code());
        return new AccessGrantedResponse(granted.hasVoted(), granted.votedOn(), granted.votingToken(),
                granted.votingTokenExpiresAt(), granted.resultsToken(), granted.resultsTokenExpiresAt());
    }

    /** RF-05b (D-48): the ID token of the institutional account, in the body (never in the URL). */
    @PostMapping("/sso")
    public AccessGrantedResponse institutionalAccount(@PathVariable String linkToken,
            @Valid @RequestBody InstitutionalAccountRequest request) {
        VoterAccessUseCase.AccessGranted granted = access.verifyInstitutionalAccount(linkToken, request.idToken());
        return new AccessGrantedResponse(granted.hasVoted(), granted.votedOn(), granted.votingToken(),
                granted.votingTokenExpiresAt(), granted.resultsToken(), granted.resultsTokenExpiresAt());
    }
}
