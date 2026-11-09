import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { callApi } from './api-error';
import { Lang } from './process-api.service';

/** `Census` of the election-service contract (RF-03). */
export interface Census {
  id: number;
  name: string;
  voterCount: number;
  frozen: boolean;
  createdAt: string;
}

/** `VoterData` of the contract: phone only digits, language es/en. */
export interface VoterData {
  fullName: string;
  email: string;
  phone: string | null;
  institutionalId: string | null;
  preferredLanguage: Lang;
}

export interface Voter extends VoterData {
  id: number;
}

export interface VoterPage {
  items: Voter[];
  page: number;
  size: number;
  totalItems: number;
}

/** Result of a CSV import or of its dry run (D-27). */
export interface ImportSummary {
  applied: boolean;
  toCreate: number;
  duplicates: { line: number; reason: 'EMAIL' | 'INSTITUTIONAL_ID' }[];
  invalidLines: { line: number; message: string }[];
  /** Lines with the example emails of the downloadable template; they are not filtered. */
  exampleLines?: number[];
}

/** Limits of D-27 checked before uploading (the service checks them again). */
export const CSV_MAX_BYTES = 5 * 1024 * 1024;

/** Census management calls (RF-03). Only a census manager changes a census; an administrator reads it. */
@Injectable({ providedIn: 'root' })
export class CensusApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${APP_CONFIG.apiBaseUrl}/api/v1/censuses`;

  list(): Promise<Census[]> {
    return callApi(this.http.get<Census[]>(this.base));
  }

  get(id: number): Promise<Census> {
    return callApi(this.http.get<Census>(`${this.base}/${id}`));
  }

  create(name: string): Promise<Census> {
    return callApi(this.http.post<Census>(this.base, { name }));
  }

  voters(id: number, page: number, size: number, search: string): Promise<VoterPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search) params = params.set('search', search);
    return callApi(this.http.get<VoterPage>(`${this.base}/${id}/voters`, { params }));
  }

  addVoter(id: number, voter: VoterData): Promise<Voter> {
    return callApi(this.http.post<Voter>(`${this.base}/${id}/voters`, voter));
  }

  updateVoter(id: number, voterId: number, voter: VoterData): Promise<Voter> {
    return callApi(this.http.put<Voter>(`${this.base}/${id}/voters/${voterId}`, voter));
  }

  removeVoter(id: number, voterId: number): Promise<void> {
    return callApi(this.http.delete<void>(`${this.base}/${id}/voters/${voterId}`));
  }

  /**
   * Uploads the CSV. `dryRun` only validates (nothing stored); otherwise it is applied in one transaction, and only
   * when there are no invalid lines or `discardInvalidLines` is set. Returned as an observable so that a dry run
   * can be cancelled by unsubscribing.
   */
  importVoters(id: number, file: File, dryRun: boolean, discardInvalidLines = false): Observable<ImportSummary> {
    const body = new FormData();
    body.append('file', file, file.name);
    const params = new HttpParams().set('dryRun', dryRun).set('discardInvalidLines', discardInvalidLines);
    return this.http.post<ImportSummary>(`${this.base}/${id}/imports`, body, { params });
  }
}
