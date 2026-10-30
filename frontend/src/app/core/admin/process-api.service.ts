import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';

export type Lang = 'es' | 'en';
export type LocalizedText = Partial<Record<Lang, string>>;
export type ProcessStatus = 'DRAFT' | 'SCHEDULED' | 'OPEN' | 'CLOSED' | 'TALLIED' | 'ARCHIVED';
export type VoteType = 'SINGLE_CHOICE' | 'YES_NO_ABSTAIN';
export type ResultsVisibility = 'CENSUS_ONLY' | 'PUBLIC_LINK';

export const PROCESS_STATUSES: readonly ProcessStatus[] = ['DRAFT', 'SCHEDULED', 'OPEN', 'CLOSED', 'TALLIED', 'ARCHIVED'];

/** `ProcessData` of the election-service contract (RF-06, RF-20). */
export interface ProcessData {
  censusId: number;
  title: LocalizedText;
  description: LocalizedText;
  opensAt: string;
  closesAt: string;
  defaultLanguage: Lang;
  resultsVisibility: ResultsVisibility;
  ballot: {
    voteType: VoteType;
    question: LocalizedText;
    blankVoteEnabled: boolean;
    options: { position: number; text: LocalizedText }[];
  };
}

export interface Process extends Omit<ProcessData, 'ballot'> {
  id: number;
  status: ProcessStatus;
  ballot: ProcessData['ballot'] & { options: { id: number; position: number; text: LocalizedText }[] };
}

export interface ProcessSummary {
  id: number;
  title: LocalizedText;
  status: ProcessStatus;
  opensAt: string;
  closesAt: string;
}

export interface StatusChange {
  from: ProcessStatus;
  to: ProcessStatus;
  changedAt: string;
  changedBy: string;
}

export interface CensusSummary {
  id: number;
  name: string;
  voterCount: number;
  frozen: boolean;
}

export interface FieldError {
  field: string;
  message: string;
}

/** Problem details of the API: status, Spanish `detail` and field errors (RNF-8). */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly detail: string | null,
    readonly errors: FieldError[],
  ) {
    super(detail ?? `API error ${status}`);
  }
}

/** Management calls for processes and lifecycle (RF-06, RF-07). The Keycloak token is added by the interceptor. */
@Injectable({ providedIn: 'root' })
export class ProcessApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${APP_CONFIG.apiBaseUrl}/api/v1`;

  list(): Promise<ProcessSummary[]> {
    return this.call(this.http.get<ProcessSummary[]>(`${this.base}/processes`));
  }

  get(id: number): Promise<Process> {
    return this.call(this.http.get<Process>(`${this.base}/processes/${id}`));
  }

  create(data: ProcessData): Promise<Process> {
    return this.call(this.http.post<Process>(`${this.base}/processes`, data));
  }

  update(id: number, data: ProcessData): Promise<Process> {
    return this.call(this.http.put<Process>(`${this.base}/processes/${id}`, data));
  }

  schedule(id: number): Promise<Process> {
    return this.call(this.http.post<Process>(`${this.base}/processes/${id}/schedule`, null));
  }

  archive(id: number): Promise<Process> {
    return this.call(this.http.post<Process>(`${this.base}/processes/${id}/archive`, null));
  }

  history(id: number): Promise<StatusChange[]> {
    return this.call(this.http.get<StatusChange[]>(`${this.base}/processes/${id}/status-history`));
  }

  censuses(): Promise<CensusSummary[]> {
    return this.call(this.http.get<CensusSummary[]>(`${this.base}/censuses`));
  }

  private async call<T>(request: Observable<T>): Promise<T> {
    try {
      return await firstValueFrom(request);
    } catch (error) {
      if (error instanceof HttpErrorResponse) {
        const body = (typeof error.error === 'object' && error.error) || {};
        throw new ApiError(error.status, (body['detail'] as string | undefined) ?? null,
          (body['errors'] as FieldError[] | undefined) ?? []);
      }
      throw error;
    }
  }
}
