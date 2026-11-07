import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '../config/app-config';
import { ProcessStatus } from './process-api.service';
import { callApi } from './api-error';

export interface PrivacyProcess {
  id: number;
  title: Record<string, string>;
  status: ProcessStatus;
}

/** One census entry of the person and what an erasure would do with it (D-46). */
export interface PrivacyEntry {
  census: string;
  fullName: string;
  email: string;
  phone: string | null;
  institutionalId: string | null;
  preferredLanguage: string;
  addedAt: string;
  erasure: 'DELETE' | 'ANONYMIZE';
  processes: PrivacyProcess[];
}

export interface PrivacySubject {
  fullName: string;
  entries: PrivacyEntry[];
  erasureAllowed: boolean;
  blockingProcesses: PrivacyProcess[];
  toDelete: number;
  toAnonymize: number;
}

/**
 * RF-18 (election-service, D-46): requests of a person about their data, for the ADMIN. The email or phone travels
 * in the body, never in the URL. The interceptor adds the Keycloak token.
 */
@Injectable({ providedIn: 'root' })
export class PrivacyApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${APP_CONFIG.apiBaseUrl}/api/v1/privacy`;

  search(query: string): Promise<PrivacySubject> {
    return callApi(this.http.post<PrivacySubject>(`${this.base}/search`, { query }));
  }

  /** The machine-readable file the person receives. */
  export(query: string): Promise<Blob> {
    return callApi(this.http.post(`${this.base}/export`, { query }, { responseType: 'blob' }));
  }

  erase(query: string): Promise<{ deleted: number; anonymized: number }> {
    return callApi(this.http.post<{ deleted: number; anonymized: number }>(`${this.base}/erasure`,
      { query, confirmed: true }));
  }
}
