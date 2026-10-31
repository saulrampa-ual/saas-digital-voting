package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.application.port.in.ManagePrivacyUseCase;
import com.saasvoting.election.domain.model.ErasureResult;
import com.saasvoting.election.domain.model.InvalidVoterDataException;
import com.saasvoting.election.domain.model.Language;
import com.saasvoting.election.domain.model.LocalizedText;
import com.saasvoting.election.domain.model.PersonalRecord;
import com.saasvoting.election.domain.model.PrivacyExport;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RF-18 (D-46): requests of a person about their data, answered by the ADMIN of the organization (checked by the use
 * case). The email or phone travels in the body, never in the URL (it would end up in access logs).
 */
@RestController
@RequestMapping("/api/v1/privacy")
public class PrivacyController {

    public record QueryRequest(String query) {
    }

    public record ErasureRequest(String query, Boolean confirmed) {
    }

    public record ProcessResponse(long id, Map<String, String> title, String status) {
    }

    public record EntryResponse(String census, String fullName, String email, String phone, String institutionalId,
            String preferredLanguage, Instant addedAt, String erasure, List<ProcessResponse> processes) {
    }

    public record SubjectResponse(String fullName, List<EntryResponse> entries, boolean erasureAllowed,
            List<ProcessResponse> blockingProcesses, int toDelete, int toAnonymize) {
    }

    public record ErasureResponse(int deleted, int anonymized) {
    }

    private final ManagePrivacyUseCase privacy;
    private final AdminActionRecorder actions;
    private final Clock clock;

    public PrivacyController(ManagePrivacyUseCase privacy, AdminActionRecorder actions, Clock clock) {
        this.privacy = Objects.requireNonNull(privacy, "privacy must not be null");
        this.actions = Objects.requireNonNull(actions, "actions must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @PostMapping("/search")
    public SubjectResponse search(@AuthenticationPrincipal Jwt jwt, @RequestBody QueryRequest request) {
        List<PersonalRecord> records = privacy.find(CallerResolver.from(jwt), request == null ? null : request.query());
        List<ProcessResponse> blocking = records.stream()
                .flatMap(record -> record.activeProcesses().stream())
                .map(PrivacyController::process)
                .distinct()
                .toList();
        int anonymize = (int) records.stream().filter(PersonalRecord::mustBeAnonymized).count();
        return new SubjectResponse(records.getLast().data().fullName(),
                records.stream().map(PrivacyController::entry).toList(), blocking.isEmpty(), blocking,
                records.size() - anonymize, anonymize);
    }

    /** The file the person receives: JSON with stable English keys and a note in Spanish and English. */
    @PostMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> export(@AuthenticationPrincipal Jwt jwt,
            @RequestBody QueryRequest request) {
        PrivacyExport export = privacy.export(CallerResolver.from(jwt), request == null ? null : request.query());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("format", "verivoto-personal-data/1");
        body.put("generatedAt", clock.instant());
        body.put("organizationId", jwt.getClaimAsString("organization_id"));
        body.put("note", Map.of(
                "es", "Datos personales que VeriVoto guarda sobre ti en los censos de esta organización. El voto no "
                        + "se incluye: es secreto y no está vinculado a tu identidad.",
                "en", "Personal data VeriVoto keeps about you in the voter lists of this organization. Your vote is "
                        + "not included: it is secret and not linked to your identity."));
        body.put("censusEntries", export.records().stream().map(record -> exportEntry(record, export)).toList());
        actions.record(jwt, "PERSONAL_DATA_EXPORTED", "VOTER", export.records().getFirst().voterId(), null,
                Integer.toString(export.records().size()), null);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=datos-personales.json")
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @PostMapping("/erasure")
    public ErasureResponse erase(@AuthenticationPrincipal Jwt jwt, @RequestBody ErasureRequest request) {
        if (request == null || !Boolean.TRUE.equals(request.confirmed())) {
            throw new InvalidVoterDataException("confirmed", "Confirma la solicitud de borrado de esta persona.");
        }
        ErasureResult result = privacy.erase(CallerResolver.from(jwt), request.query());
        actions.record(jwt, "PERSONAL_DATA_ERASED", "VOTER", result.firstVoterId(), null,
                result.deleted() + ":" + result.anonymized(), null);
        return new ErasureResponse(result.deleted(), result.anonymized());
    }

    private static EntryResponse entry(PersonalRecord record) {
        return new EntryResponse(record.censusName(), record.data().fullName(), record.data().email(),
                record.data().phone(), record.data().institutionalId(), record.data().language().code(),
                record.createdAt(), record.mustBeAnonymized() ? "ANONYMIZE" : "DELETE",
                record.processes().stream().map(PrivacyController::process).toList());
    }

    private static ProcessResponse process(PersonalRecord.ProcessRef process) {
        return new ProcessResponse(process.id(), texts(process.title()), process.status().name());
    }

    private static Map<String, Object> exportEntry(PersonalRecord record, PrivacyExport export) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("census", record.censusName());
        entry.put("fullName", record.data().fullName());
        entry.put("email", record.data().email());
        entry.put("phone", record.data().phone());
        entry.put("institutionalId", record.data().institutionalId());
        entry.put("preferredLanguage", record.data().language().code());
        entry.put("addedAt", record.createdAt());
        entry.put("processes", record.processes().stream().map(process -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", texts(process.title()));
            item.put("status", process.status().name());
            item.put("opensAt", process.opensAt());
            item.put("closesAt", process.closesAt());
            export.roll().stream()
                    .filter(roll -> roll.processId() == process.id() && roll.voterRef() == record.voterId())
                    .findFirst()
                    .ifPresent(roll -> {
                        Map<String, Object> participation = new LinkedHashMap<>();
                        participation.put("voted", roll.hasVoted());
                        participation.put("votedOn", roll.votedOn());
                        item.put("participation", participation);
                        item.put("emails", roll.emails());
                    });
            return item;
        }).toList());
        return entry;
    }

    private static Map<String, String> texts(LocalizedText text) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Language language : Language.values()) {
            text.get(language).ifPresent(value -> result.put(language.code(), value));
        }
        return result;
    }
}
