import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '../config/app-config';
import { callApi } from '../admin/api-error';
import { CountedVotes, Result } from './results.model';

/** V8: result through the public link (D-16). No token: the link is the only key. */
@Injectable({ providedIn: 'root' })
export class PublicResultsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${APP_CONFIG.apiBaseUrl}/api/v1/ballot-box/public/results`;

  result(publicId: string): Promise<Result> {
    return callApi(this.http.get<Result>(`${this.base}/${encodeURIComponent(publicId)}`));
  }

  countedVotes(publicId: string): Promise<CountedVotes> {
    return callApi(this.http.get<CountedVotes>(`${this.base}/${encodeURIComponent(publicId)}/counted-votes`));
  }
}
