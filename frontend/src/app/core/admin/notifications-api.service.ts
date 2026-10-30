import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '../config/app-config';
import { callApi } from './api-error';

export type NotificationType = 'INVITATION' | 'REMINDER' | 'CLOSING';
export type ReminderStatus = 'SCHEDULED' | 'SENT' | 'CANCELLED';

export interface NotificationCounts {
  pending: number;
  delivered: number;
  failed: number;
  /** Not needed any more: the person had already voted or voting was over (D-45). */
  cancelled: number;
}

/** A failed invitation or closing notice. Failed reminders are only counted (D-45). */
export interface FailedNotification {
  type: NotificationType;
  displayName: string;
  email: string;
  attempts: number;
  reason: string | null;
  createdAt: string;
}

export interface Reminder {
  id: number;
  sendAt: string;
  status: ReminderStatus;
  /** How many people it went to; never who. */
  recipients: number | null;
  createdAt: string;
}

export interface NotificationSummary {
  processId: number;
  status: string;
  opensAt: string;
  closesAt: string;
  counts: Record<NotificationType, NotificationCounts>;
  failed: FailedNotification[];
  reminders: Reminder[];
  maxReminders: number;
}

/** RF-17 (voter-access, D-45): emails of a process for its ADMIN. The interceptor adds the Keycloak token. */
@Injectable({ providedIn: 'root' })
export class NotificationsApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${APP_CONFIG.apiBaseUrl}/api/v1/voter-access/processes`;

  summary(processId: number): Promise<NotificationSummary> {
    return callApi(this.http.get<NotificationSummary>(`${this.base}/${processId}/notifications`));
  }

  /** Without a date the reminder goes out now. */
  scheduleReminder(processId: number, sendAt?: string): Promise<Reminder> {
    return callApi(this.http.post<Reminder>(`${this.base}/${processId}/reminders`, sendAt ? { sendAt } : {}));
  }

  cancelReminder(processId: number, reminderId: number): Promise<Reminder> {
    return callApi(this.http.delete<Reminder>(`${this.base}/${processId}/reminders/${reminderId}`));
  }

  retryFailed(processId: number): Promise<{ retried: number }> {
    return callApi(this.http.post<{ retried: number }>(`${this.base}/${processId}/notifications/retry`, {}));
  }
}
