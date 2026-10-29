package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.application.port.in.ImportVotersUseCase;
import com.saasvoting.election.application.port.in.ManageCensusesUseCase;
import com.saasvoting.election.application.port.in.ManageVotersUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/** RF-03. Contract: {@code openapi/election-service.yaml}. */
@RestController
@RequestMapping("/api/v1/censuses")
public class CensusController {

    private final ManageCensusesUseCase censuses;
    private final ManageVotersUseCase voters;
    private final ImportVotersUseCase imports;

    public CensusController(ManageCensusesUseCase censuses, ManageVotersUseCase voters, ImportVotersUseCase imports) {
        this.censuses = Objects.requireNonNull(censuses, "censuses must not be null");
        this.voters = Objects.requireNonNull(voters, "voters must not be null");
        this.imports = Objects.requireNonNull(imports, "imports must not be null");
    }

    @GetMapping
    public List<CensusResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return censuses.list(CallerResolver.from(jwt)).stream().map(CensusResponse::fromView).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CensusResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CensusRequest request) {
        return CensusResponse.fromView(censuses.create(CallerResolver.from(jwt), request.name()));
    }

    @GetMapping("/{censusId}")
    public CensusResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable long censusId) {
        return CensusResponse.fromView(censuses.get(CallerResolver.from(jwt), censusId));
    }

    @GetMapping("/{censusId}/voters")
    public VoterPageResponse listVoters(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long censusId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search) {
        Paging paging = Paging.of(page, size);
        return VoterPageResponse.fromDomain(
                voters.list(CallerResolver.from(jwt), censusId, search, paging.page(), paging.size()));
    }

    @PostMapping("/{censusId}/voters")
    public ResponseEntity<VoterResponse> addVoter(
            @AuthenticationPrincipal Jwt jwt, @PathVariable long censusId, @Valid @RequestBody VoterRequest request) {
        VoterResponse voter = VoterResponse.fromDomain(voters.add(CallerResolver.from(jwt), censusId, request.toDomain()));
        return ResponseEntity.created(URI.create("/api/v1/censuses/" + censusId + "/voters/" + voter.id())).body(voter);
    }

    @PutMapping("/{censusId}/voters/{voterId}")
    public VoterResponse updateVoter(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long censusId,
            @PathVariable long voterId,
            @Valid @RequestBody VoterRequest request) {
        return VoterResponse.fromDomain(
                voters.update(CallerResolver.from(jwt), censusId, voterId, request.toDomain()));
    }

    @DeleteMapping("/{censusId}/voters/{voterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeVoter(@AuthenticationPrincipal Jwt jwt, @PathVariable long censusId, @PathVariable long voterId) {
        voters.remove(CallerResolver.from(jwt), censusId, voterId);
    }

    @PostMapping(path = "/{censusId}/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportSummaryResponse importVoters(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long censusId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(defaultValue = "false") boolean discardInvalidLines) throws IOException {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return ImportSummaryResponse.fromDomain(imports.importVoters(
                    CallerResolver.from(jwt), censusId, VoterCsvReader.read(reader), dryRun, discardInvalidLines));
        }
    }
}
