package com.saasvoting.voteraccess.infrastructure.adapter.in.rest;

import com.saasvoting.voteraccess.application.port.in.PersonalDataUseCase;
import com.saasvoting.voteraccess.domain.model.PersonalDataRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * RF-18 (D-46): election-service, which owns the census, asks for the data kept about a person and has the roll
 * anonymized after an erasure (through its outbox). Only the client election-service (IMP-26).
 */
@RestController
@RequestMapping("/internal/v1")
public class InternalPersonalDataController {

    public record RollRefRequest(@NotNull Long processId, @NotNull Long voterRef) {
    }

    public record RecordsRequest(@NotEmpty @Size(max = 500) List<@NotNull @Valid RollRefRequest> entries) {
    }

    public record EmailResponse(String type, String status, Instant createdAt, Instant sentAt) {
    }

    public record RecordResponse(long processId, long voterRef, boolean hasVoted, LocalDate votedOn,
            List<EmailResponse> emails) {
    }

    public record RecordsResponse(List<RecordResponse> entries) {
    }

    public record AnonymizationRequest(@NotEmpty @Size(max = 500) List<@NotNull Long> voterRefs) {
    }

    private final PersonalDataUseCase personalData;

    public InternalPersonalDataController(PersonalDataUseCase personalData) {
        this.personalData = Objects.requireNonNull(personalData, "personalData must not be null");
    }

    @PostMapping("/privacy/records")
    public RecordsResponse records(@Valid @RequestBody RecordsRequest request) {
        List<PersonalDataRecord> records = personalData.records(request.entries().stream()
                .map(entry -> new PersonalDataUseCase.RollRef(entry.processId(), entry.voterRef()))
                .toList());
        return new RecordsResponse(records.stream()
                .map(record -> new RecordResponse(record.processId(), record.voterRef(), record.hasVoted(),
                        record.votedOn(), record.emails().stream()
                                .map(email -> new EmailResponse(email.type().name(), email.status().name(),
                                        email.createdAt(), email.sentAt()))
                                .toList()))
                .toList());
    }

    @PutMapping("/processes/{processId}/roll/anonymization")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void anonymize(@PathVariable long processId, @Valid @RequestBody AnonymizationRequest request) {
        personalData.anonymize(processId, request.voterRefs());
    }
}
