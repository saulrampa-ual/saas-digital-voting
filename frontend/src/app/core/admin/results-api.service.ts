import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '../config/app-config';
import { ApiError, callApi } from './api-error';
import { CountedVotes, Result } from '../results/results.model';

export type { CountedVotes, Result } from '../results/results.model';

/** RF-14 (voter-access): counts only, by day (D-18). */
export interface Participation {
  processId: number;
  status: string;
  opensAt: string;
  closesAt: string;
  eligibleVoters: number;
  votesCast: number;
  pending: number;
  participation: number;
  byDay: { day: string; votes: number; cumulative: number }[];
}

/** RF-12 (ballot-box): one event of the hash chain; vote events have no time (D-18). */
export interface AuditEvent {
  sequence: number;
  type: 'OPENED' | 'VOTE_RECEIVED' | 'CLOSED' | 'FINAL_HASH_PUBLISHED';
  occurredAt: string | null;
  payloadHash: string;
  previousHash: string;
  eventHash: string;
}

export interface AuditEventPage {
  items: AuditEvent[];
  page: number;
  size: number;
  totalItems: number;
  status: string;
  closesAt: string;
  finalHash: string | null;
}

export interface ChainCheck {
  processId: number;
  intact: boolean;
  eventsChecked: number;
  checkedAt: string;
  finalHash: string | null;
  problems: { sequence: number | null; code: string }[];
}

/** Management reads of results, audit chain and participation. The interceptor adds the Keycloak token. */
@Injectable({ providedIn: 'root' })
export class ResultsApiService {
  private readonly http = inject(HttpClient);
  private readonly ballotBox = `${APP_CONFIG.apiBaseUrl}/api/v1/ballot-box/processes`;
  private readonly voterAccess = `${APP_CONFIG.apiBaseUrl}/api/v1/voter-access/processes`;

  /** Null while there is no result yet (409: closed and the tally still running, RF-15 exception 2). */
  async result(processId: number): Promise<Result | null> {
    try {
      return await callApi(this.http.get<Result>(`${this.ballotBox}/${processId}/results`));
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) return null;
      throw error;
    }
  }

  countedVotes(processId: number): Promise<CountedVotes> {
    return callApi(this.http.get<CountedVotes>(`${this.ballotBox}/${processId}/results/counted-votes`));
  }

  /** RF-15: CSV for ADMIN (separator `;`, decimal comma). */
  exportCsv(processId: number, lang: string): Promise<Blob> {
    return callApi(this.http.get(`${this.ballotBox}/${processId}/results/export`, {
      params: new HttpParams().set('lang', lang), responseType: 'blob',
    }));
  }

  /** RF-16: minutes in PDF for ADMIN; the same file every time, with its SHA-256 (D-40). */
  async minutes(processId: number, lang: string): Promise<{ file: Blob; sha256: string | null }> {
    const response = await callApi(this.http.get(`${this.ballotBox}/${processId}/results/minutes`, {
      params: new HttpParams().set('lang', lang), responseType: 'blob', observe: 'response',
    }));
    return { file: response.body ?? new Blob(), sha256: response.headers.get('X-Content-SHA256') };
  }

  auditEvents(processId: number, page: number, size: number): Promise<AuditEventPage> {
    return callApi(this.http.get<AuditEventPage>(`${this.ballotBox}/${processId}/audit-events`, {
      params: new HttpParams().set('page', page).set('size', size),
    }));
  }

  verifyChain(processId: number): Promise<ChainCheck> {
    return callApi(this.http.post<ChainCheck>(`${this.ballotBox}/${processId}/audit-events/verification`, null));
  }

  /** Null if the chain was never verified (404). */
  async lastCheck(processId: number): Promise<ChainCheck | null> {
    try {
      return await callApi(this.http.get<ChainCheck>(`${this.ballotBox}/${processId}/audit-events/verification`));
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) return null;
      throw error;
    }
  }

  participation(processId: number): Promise<Participation> {
    return callApi(this.http.get<Participation>(`${this.voterAccess}/${processId}/participation`));
  }
}
