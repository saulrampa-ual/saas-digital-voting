import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '../config/app-config';
import { callApi } from './api-error';

/** Codes of the administrative actions registered by the services (RF-19, D-44). */
export const ADMIN_ACTIONS = [
  'ORGANIZATION_CREATED', 'ORGANIZATION_STATUS_CHANGED', 'USER_CREATED', 'USER_ROLE_CHANGED',
  'CENSUS_CREATED', 'VOTER_ADDED', 'VOTER_UPDATED', 'VOTER_REMOVED', 'VOTERS_IMPORTED',
  'PROCESS_CREATED', 'PROCESS_UPDATED', 'PROCESS_SCHEDULED', 'PROCESS_ARCHIVED',
  'CHAIN_VERIFIED', 'RESULTS_EXPORTED', 'MINUTES_DOWNLOADED',
  'REMINDER_SCHEDULED', 'REMINDER_CANCELLED', 'NOTIFICATIONS_RETRIED', 'PERSONAL_DATA_EXPORTED',
  'PERSONAL_DATA_ERASED', 'API_KEY_CREATED', 'API_KEY_REVOKED',
] as const;

/** RF-19: who did what, on which object and when. Read only. */
export interface AdminAction {
  occurredAt: string;
  actorId: string;
  actorName: string;
  action: string;
  objectType: string;
  objectId: string | null;
  objectLabel: string | null;
  detail: string | null;
  processId: number | null;
  source: string;
}

export interface AdminActionPage {
  items: AdminAction[];
  page: number;
  size: number;
  totalItems: number;
}

export interface Actor {
  actorId: string;
  actorName: string;
}

export interface AdminActionFilter {
  from?: string;
  to?: string;
  actorId?: string;
  action?: string;
  processId?: number;
}

/** RF-19 (identity-service): ADMIN and AUDITOR of the organization in the token. The interceptor adds the token. */
@Injectable({ providedIn: 'root' })
export class AdminActionsApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${APP_CONFIG.apiBaseUrl}/api/v1/admin-actions`;

  search(filter: AdminActionFilter, page: number, size: number): Promise<AdminActionPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    for (const [key, value] of Object.entries(filter)) {
      if (value !== undefined && value !== null && value !== '') params = params.set(key, String(value));
    }
    return callApi(this.http.get<AdminActionPage>(this.base, { params }));
  }

  actors(): Promise<Actor[]> {
    return callApi(this.http.get<Actor[]>(`${this.base}/actors`));
  }
}
