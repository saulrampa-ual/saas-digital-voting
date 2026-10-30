import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { CountedVotes, Result } from '../results/results.model';

export type LocalizedText = Partial<Record<'es' | 'en', string>>;
export type ProcessStatus = 'SCHEDULED' | 'OPEN' | 'CLOSED' | 'TALLIED' | 'ARCHIVED';

/** voter-access `LinkStatus` (screens V1, C3, V4). */
export interface LinkStatus {
  processId: number;
  title: LocalizedText;
  status: ProcessStatus;
  opensAt: string;
  closesAt: string;
  defaultLanguage: 'es' | 'en';
}

export interface CodeSent {
  maskedEmail: string;
  expiresAt: string;
  resendAvailableAt: string;
}

export interface AccessGranted {
  hasVoted: boolean;
  votedOn: string | null;
  votingToken: string | null;
  votingTokenExpiresAt: string | null;
  /** Only once tallied (D-41): sent to ballot-box in a header to see the result. */
  resultsToken?: string | null;
  resultsTokenExpiresAt?: string | null;
}

export interface BallotOption {
  id: number;
  position: number;
  text: LocalizedText;
}

/** ballot-box `Ballot` with every translation, so changing language keeps the selection (RF-20). */
export interface Ballot {
  processId: number;
  voteType: 'SINGLE_CHOICE' | 'YES_NO_ABSTAIN';
  question: LocalizedText;
  options: BallotOption[];
  blankVoteEnabled: boolean;
  closesAt: string;
}

export type Selection = { optionId: number } | { blank: true };

/** Normalized API error: HTTP status (0 = no connection) and the extra fields of the problem details. */
export class VoterApiError extends Error {
  constructor(
    readonly status: number,
    readonly problem: Record<string, unknown>,
  ) {
    super(`Voter API error ${status}`);
  }

  get processStatus(): ProcessStatus | null {
    return (this.problem['processStatus'] as ProcessStatus | undefined) ?? null;
  }
}

/**
 * Calls of the voter PWA to voter-access (RF-08) and ballot-box (RF-09, RF-10, RF-15 with D-41). No Keycloak token is
 * ever attached (see app.config). The personal link, the code, the voting token and the results token are kept only
 * in memory, never stored.
 */
@Injectable({ providedIn: 'root' })
export class VoterApiService {
  private readonly http = inject(HttpClient);
  private readonly access = `${APP_CONFIG.apiBaseUrl}/api/v1/voter-access/links`;
  private readonly ballotBox = `${APP_CONFIG.apiBaseUrl}/api/v1/ballot-box/processes`;
  private readonly voterResults = `${APP_CONFIG.apiBaseUrl}/api/v1/ballot-box/voter/processes`;

  linkStatus(linkToken: string): Promise<LinkStatus> {
    return this.call(this.http.get<LinkStatus>(`${this.access}/${encodeURIComponent(linkToken)}`));
  }

  sendCode(linkToken: string): Promise<CodeSent> {
    return this.call(this.http.post<CodeSent>(`${this.access}/${encodeURIComponent(linkToken)}/otp`, null));
  }

  verifyCode(linkToken: string, code: string): Promise<AccessGranted> {
    return this.call(
      this.http.post<AccessGranted>(`${this.access}/${encodeURIComponent(linkToken)}/otp/verify`, { code }),
    );
  }

  /** IMP-31: the ballot is given only with a valid voting token, sent in a header (never in the URL). */
  ballot(processId: number, votingToken: string): Promise<Ballot> {
    return this.call(this.http.get<Ballot>(`${this.ballotBox}/${processId}/ballot`, {
      headers: new HttpHeaders({ 'X-Voting-Token': votingToken }),
    }));
  }

  castVote(processId: number, votingToken: string, selection: Selection): Promise<{ receiptCode: string }> {
    return this.call(
      this.http.post<{ receiptCode: string }>(`${this.ballotBox}/${processId}/votes`, { votingToken, selection }),
    );
  }

  receiptPresent(processId: number, receiptCode: string): Promise<boolean> {
    return this.call(
      this.http.get<{ present: boolean }>(
        `${this.ballotBox}/${processId}/receipts/${encodeURIComponent(receiptCode)}`,
      ),
    ).then((result) => result.present);
  }

  /** D-41: the result for a person of the roll. The token goes in a header, never in the URL. */
  result(processId: number, resultsToken: string): Promise<Result> {
    return this.call(this.http.get<Result>(`${this.voterResults}/${processId}/results`, {
      headers: new HttpHeaders({ 'X-Results-Token': resultsToken }),
    }));
  }

  countedVotes(processId: number, resultsToken: string): Promise<CountedVotes> {
    return this.call(this.http.get<CountedVotes>(`${this.voterResults}/${processId}/results/counted-votes`, {
      headers: new HttpHeaders({ 'X-Results-Token': resultsToken }),
    }));
  }

  private async call<T>(request: Observable<T>): Promise<T> {
    try {
      return await firstValueFrom(request);
    } catch (error) {
      if (error instanceof HttpErrorResponse) {
        const problem = typeof error.error === 'object' && error.error !== null ? error.error : {};
        throw new VoterApiError(error.status, problem as Record<string, unknown>);
      }
      throw error;
    }
  }
}
