import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { VoterApiService } from './voter-api.service';

describe('VoterApiService voting flow', () => {
  let api: VoterApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(VoterApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends the token in the ballot header, casts a vote and verifies only receipt presence', async () => {
    const token = 'synthetic-voting-token';
    const receipt = 'a'.repeat(64);
    const ballotPromise = api.ballot(42, token);
    const ballotRequest = http.expectOne('/api/v1/ballot-box/processes/42/ballot');
    expect(ballotRequest.request.headers.get('X-Voting-Token')).toBe(token);
    ballotRequest.flush({
      processId: 42,
      voteType: 'SINGLE_CHOICE',
      question: { es: 'Pregunta sintética' },
      options: [{ id: 7, position: 1, text: { es: 'Opción sintética' } }],
      blankVoteEnabled: false,
      closesAt: '2026-11-01T12:00:00Z',
    });
    await expect(ballotPromise).resolves.toMatchObject({ processId: 42 });

    const votePromise = api.castVote(42, token, { optionId: 7 });
    const voteRequest = http.expectOne('/api/v1/ballot-box/processes/42/votes');
    expect(voteRequest.request.method).toBe('POST');
    expect(voteRequest.request.body).toEqual({ votingToken: token, selection: { optionId: 7 } });
    voteRequest.flush({ receiptCode: receipt });
    await expect(votePromise).resolves.toEqual({ receiptCode: receipt });

    const verificationPromise = api.receiptPresent(42, receipt);
    const verificationRequest = http.expectOne(`/api/v1/ballot-box/processes/42/receipts/${receipt}`);
    verificationRequest.flush({ present: true });
    await expect(verificationPromise).resolves.toBe(true);
  });

  it('keeps the personal link out of the URL when requesting an access code', async () => {
    const link = 'synthetic/link-token';
    const promise = api.sendCode(link);
    const request = http.expectOne('/api/v1/voter-access/links/synthetic%2Flink-token/otp');
    expect(request.request.url).not.toContain(link);
    request.flush({ maskedEmail: '***@example.test', expiresAt: '2026-10-31T13:00:00Z',
      resendAvailableAt: '2026-10-31T12:01:00Z' });
    await expect(promise).resolves.toMatchObject({ maskedEmail: '***@example.test' });
  });
});
