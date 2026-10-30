package com.saasvoting.voteraccess.infrastructure.adapter.in.rest;

import com.saasvoting.voteraccess.application.port.in.ManageNotificationsUseCase;
import com.saasvoting.voteraccess.domain.model.NotificationStatus;
import com.saasvoting.voteraccess.domain.model.NotificationSummary;
import com.saasvoting.voteraccess.domain.model.Reminder;
import com.saasvoting.voteraccess.domain.model.SnapshotNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RF-17 (D-45): emails of a process for the ADMIN of the organization (role checked by the security filter). Every
 * change is recorded for the traceability (RF-19).
 */
@RestController
public class NotificationController {

    public record CountsResponse(int pending, int delivered, int failed, int cancelled) {
    }

    public record FailedResponse(String type, String displayName, String email, int attempts, String reason,
            Instant createdAt) {
    }

    public record ReminderResponse(long id, Instant sendAt, String status, Integer recipients, Instant createdAt) {
    }

    public record NotificationsResponse(long processId, String status, Instant opensAt, Instant closesAt,
            Map<String, CountsResponse> counts, List<FailedResponse> failed, List<ReminderResponse> reminders,
            int maxReminders) {
    }

    public record ReminderRequest(Instant sendAt) {
    }

    public record RetryResponse(int retried) {
    }

    private final ManageNotificationsUseCase notifications;
    private final AdminActionRecorder recorder;

    public NotificationController(ManageNotificationsUseCase notifications, AdminActionRecorder recorder) {
        this.notifications = Objects.requireNonNull(notifications, "notifications must not be null");
        this.recorder = Objects.requireNonNull(recorder, "recorder must not be null");
    }

    @GetMapping("/api/v1/voter-access/processes/{processId}/notifications")
    public NotificationsResponse summary(@AuthenticationPrincipal Jwt jwt, @PathVariable long processId) {
        NotificationSummary summary = notifications.summary(organization(jwt, processId), processId);
        Map<String, CountsResponse> counts = new LinkedHashMap<>();
        summary.counts().forEach((type, byStatus) -> counts.put(type.name(), new CountsResponse(
                byStatus.getOrDefault(NotificationStatus.PENDING, 0),
                byStatus.getOrDefault(NotificationStatus.DELIVERED, 0),
                byStatus.getOrDefault(NotificationStatus.FAILED, 0),
                byStatus.getOrDefault(NotificationStatus.CANCELLED, 0))));
        return new NotificationsResponse(summary.processId(), summary.status().name(), summary.opensAt(),
                summary.closesAt(), counts,
                summary.failed().stream().map(failed -> new FailedResponse(failed.type().name(), failed.displayName(),
                        failed.email(), failed.attempts(), failed.reason(), failed.createdAt())).toList(),
                summary.reminders().stream().map(NotificationController::reminder).toList(),
                Reminder.MAX_SCHEDULED);
    }

    @PostMapping("/api/v1/voter-access/processes/{processId}/reminders")
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponse schedule(@AuthenticationPrincipal Jwt jwt, @PathVariable long processId,
            @RequestBody(required = false) ReminderRequest request) {
        Reminder reminder = notifications.scheduleReminder(organization(jwt, processId), processId,
                request == null ? null : request.sendAt());
        recorder.record(jwt, "REMINDER_SCHEDULED", "PROCESS", processId, null, reminder.sendAt().toString(),
                processId);
        return reminder(reminder);
    }

    @DeleteMapping("/api/v1/voter-access/processes/{processId}/reminders/{reminderId}")
    public ReminderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable long processId,
            @PathVariable long reminderId) {
        Reminder reminder = notifications.cancelReminder(organization(jwt, processId), processId, reminderId);
        recorder.record(jwt, "REMINDER_CANCELLED", "PROCESS", processId, null, reminder.sendAt().toString(),
                processId);
        return reminder(reminder);
    }

    @PostMapping("/api/v1/voter-access/processes/{processId}/notifications/retry")
    public RetryResponse retry(@AuthenticationPrincipal Jwt jwt, @PathVariable long processId) {
        int retried = notifications.retryFailed(organization(jwt, processId), processId);
        recorder.record(jwt, "NOTIFICATIONS_RETRIED", "PROCESS", processId, null, Integer.toString(retried),
                processId);
        return new RetryResponse(retried);
    }

    private static ReminderResponse reminder(Reminder reminder) {
        return new ReminderResponse(reminder.id(), reminder.sendAt(), reminder.status().name(), reminder.recipients(),
                reminder.createdAt());
    }

    private static String organization(Jwt jwt, long processId) {
        String organizationId = jwt == null ? null : jwt.getClaimAsString("organization_id");
        if (organizationId == null || organizationId.isBlank()) {
            throw new SnapshotNotFoundException(processId);
        }
        return organizationId;
    }
}
