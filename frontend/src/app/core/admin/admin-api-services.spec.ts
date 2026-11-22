import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { PublicResultsService } from '../results/public-results.service';
import { AdminActionsApiService } from './admin-actions-api.service';
import { ApiError, toApiError } from './api-error';
import { ApiKeysApiService } from './api-keys-api.service';
import { CensusApiService } from './census-api.service';
import { NotificationsApiService } from './notifications-api.service';
import { PrivacyApiService } from './privacy-api.service';
import { ResultsApiService } from './results-api.service';

/**
 * Management API clients: method, URL, parameters and body of each request, and the translation of the problem
 * details into {@link ApiError}. The HTTP backend is the Angular testing double; no real service is reached.
 */
describe('management API clients', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  describe('ResultsApiService', () => {
    let api: ResultsApiService;
    beforeEach(() => (api = TestBed.inject(ResultsApiService)));

    it('reads the result, and answers null while the tally is still running (409)', async () => {
      const first = api.result(5);
      http.expectOne('/api/v1/ballot-box/processes/5/results').flush({ processId: 5 });
      expect(await first).toEqual({ processId: 5 });

      const running = api.result(5);
      http.expectOne('/api/v1/ballot-box/processes/5/results').flush({ detail: 'pending' }, { status: 409, statusText: 'Conflict' });
      expect(await running).toBeNull();

      const failing = api.result(5);
      http.expectOne('/api/v1/ballot-box/processes/5/results').flush({ detail: 'boom' }, { status: 500, statusText: 'Error' });
      await expect(failing).rejects.toBeInstanceOf(ApiError);
    });

    it('downloads the counted votes, the CSV and the minutes with its SHA-256', async () => {
      const counted = api.countedVotes(5);
      http.expectOne('/api/v1/ballot-box/processes/5/results/counted-votes').flush({ votes: [] });
      expect(await counted).toEqual({ votes: [] });

      const csv = api.exportCsv(5, 'en');
      const csvRequest = http.expectOne((r) => r.url === '/api/v1/ballot-box/processes/5/results/export');
      expect(csvRequest.request.params.get('lang')).toBe('en');
      csvRequest.flush(new Blob(['a;b']));
      expect(await csv).toBeInstanceOf(Blob);

      const minutes = api.minutes(5, 'es');
      const minutesRequest = http.expectOne((r) => r.url === '/api/v1/ballot-box/processes/5/results/minutes');
      expect(minutesRequest.request.params.get('lang')).toBe('es');
      minutesRequest.flush(new Blob(['%PDF']), { headers: { 'X-Content-SHA256': 'abc' } });
      const { file, sha256 } = await minutes;
      expect(file).toBeInstanceOf(Blob);
      expect(sha256).toBe('abc');
    });

    it('pages the audit events, verifies the chain and reads the last check (null if never verified)', async () => {
      const events = api.auditEvents(5, 2, 25);
      const eventsRequest = http.expectOne((r) => r.url === '/api/v1/ballot-box/processes/5/audit-events');
      expect(eventsRequest.request.params.get('page')).toBe('2');
      expect(eventsRequest.request.params.get('size')).toBe('25');
      eventsRequest.flush({ items: [] });
      expect(await events).toEqual({ items: [] });

      const verify = api.verifyChain(5);
      const verifyRequest = http.expectOne('/api/v1/ballot-box/processes/5/audit-events/verification');
      expect(verifyRequest.request.method).toBe('POST');
      verifyRequest.flush({ intact: true });
      expect(await verify).toEqual({ intact: true });

      const last = api.lastCheck(5);
      http.expectOne('/api/v1/ballot-box/processes/5/audit-events/verification').flush({ intact: false });
      expect(await last).toEqual({ intact: false });

      const never = api.lastCheck(5);
      http.expectOne('/api/v1/ballot-box/processes/5/audit-events/verification').flush({}, { status: 404, statusText: 'Not Found' });
      expect(await never).toBeNull();

      const failing = api.lastCheck(5);
      http.expectOne('/api/v1/ballot-box/processes/5/audit-events/verification').flush({}, { status: 503, statusText: 'Unavailable' });
      await expect(failing).rejects.toMatchObject({ status: 503 });
    });

    it('reads the participation from voter-access', async () => {
      const participation = api.participation(5);
      http.expectOne('/api/v1/voter-access/processes/5/participation').flush({ votesCast: 3 });
      expect(await participation).toEqual({ votesCast: 3 });
    });
  });

  describe('NotificationsApiService', () => {
    let api: NotificationsApiService;
    beforeEach(() => (api = TestBed.inject(NotificationsApiService)));

    it('reads the summary, schedules (now or later), cancels reminders and retries failed messages', async () => {
      const summary = api.summary(3);
      http.expectOne('/api/v1/voter-access/processes/3/notifications').flush({ processId: 3 });
      expect(await summary).toEqual({ processId: 3 });

      const now = api.scheduleReminder(3);
      const nowRequest = http.expectOne('/api/v1/voter-access/processes/3/reminders');
      expect(nowRequest.request.body).toEqual({});
      nowRequest.flush({ id: 1 });
      await now;

      const later = api.scheduleReminder(3, '2026-12-01T10:00:00Z');
      const laterRequest = http.expectOne('/api/v1/voter-access/processes/3/reminders');
      expect(laterRequest.request.body).toEqual({ sendAt: '2026-12-01T10:00:00Z' });
      laterRequest.flush({ id: 2 });
      await later;

      const cancel = api.cancelReminder(3, 2);
      const cancelRequest = http.expectOne('/api/v1/voter-access/processes/3/reminders/2');
      expect(cancelRequest.request.method).toBe('DELETE');
      cancelRequest.flush({ id: 2, status: 'CANCELLED' });
      await cancel;

      const retry = api.retryFailed(3);
      http.expectOne('/api/v1/voter-access/processes/3/notifications/retry').flush({ retried: 4 });
      expect(await retry).toEqual({ retried: 4 });
    });
  });

  describe('AdminActionsApiService', () => {
    it('sends only the filters that have a value, and lists the actors', async () => {
      const api = TestBed.inject(AdminActionsApiService);
      const search = api.search({ from: '2026-11-01T00:00:00Z', actorId: '', action: 'CENSUS_CREATED', processId: 4 }, 1, 20);
      const request = http.expectOne((r) => r.url === '/api/v1/admin-actions');
      const params = request.request.params;
      expect(params.get('page')).toBe('1');
      expect(params.get('size')).toBe('20');
      expect(params.get('from')).toBe('2026-11-01T00:00:00Z');
      expect(params.get('action')).toBe('CENSUS_CREATED');
      expect(params.get('processId')).toBe('4');
      expect(params.has('actorId')).toBe(false);
      request.flush({ items: [], totalItems: 0 });
      await search;

      const actors = api.actors();
      http.expectOne('/api/v1/admin-actions/actors').flush([{ actorId: 'a', actorName: 'Ana' }]);
      expect(await actors).toHaveLength(1);
    });
  });

  describe('CensusApiService', () => {
    let api: CensusApiService;
    beforeEach(() => (api = TestBed.inject(CensusApiService)));

    it('lists, reads and creates censuses', async () => {
      const list = api.list();
      http.expectOne('/api/v1/censuses').flush([]);
      await list;
      const get = api.get(7);
      http.expectOne('/api/v1/censuses/7').flush({ id: 7 });
      await get;
      const create = api.create('Censo');
      const request = http.expectOne('/api/v1/censuses');
      expect(request.request.body).toEqual({ name: 'Censo' });
      request.flush({ id: 8 });
      expect(await create).toEqual({ id: 8 });
    });

    it('pages and searches the voters, and adds, edits and removes one', async () => {
      const plain = api.voters(7, 0, 25, '');
      const plainRequest = http.expectOne((r) => r.url === '/api/v1/censuses/7/voters');
      expect(plainRequest.request.params.has('search')).toBe(false);
      plainRequest.flush({ items: [] });
      await plain;

      const searched = api.voters(7, 1, 25, 'ana');
      const searchedRequest = http.expectOne((r) => r.url === '/api/v1/censuses/7/voters');
      expect(searchedRequest.request.params.get('search')).toBe('ana');
      searchedRequest.flush({ items: [] });
      await searched;

      const voter = { fullName: 'Ana', email: 'ana@example.com', phone: null, institutionalId: null, preferredLanguage: 'es' as const };
      const add = api.addVoter(7, voter);
      http.expectOne((r) => r.method === 'POST' && r.url === '/api/v1/censuses/7/voters').flush({ id: 1, ...voter });
      await add;
      const update = api.updateVoter(7, 1, voter);
      http.expectOne((r) => r.method === 'PUT' && r.url === '/api/v1/censuses/7/voters/1').flush({ id: 1, ...voter });
      await update;
      const remove = api.removeVoter(7, 1);
      http.expectOne((r) => r.method === 'DELETE' && r.url === '/api/v1/censuses/7/voters/1').flush(null);
      await remove;
    });

    it('uploads the file with the dry-run and discard flags', async () => {
      const file = new File(['x'], 'censo.xlsx');
      const upload = firstValueFrom(api.importVoters(7, file, true));
      const request = http.expectOne((r) => r.url === '/api/v1/censuses/7/imports');
      expect(request.request.params.get('dryRun')).toBe('true');
      expect(request.request.params.get('discardInvalidLines')).toBe('false');
      expect((request.request.body as FormData).get('file')).toBeInstanceOf(File);
      request.flush({ applied: false, toCreate: 1, duplicates: [], invalidLines: [] });
      expect((await upload).toCreate).toBe(1);
    });

    it('turns the problem details into an ApiError with its field errors', async () => {
      const create = api.create('');
      http.expectOne('/api/v1/censuses').flush(
        { detail: 'El nombre es obligatorio', errors: [{ field: 'name', message: 'Obligatorio' }] },
        { status: 422, statusText: 'Unprocessable' },
      );
      await expect(create).rejects.toMatchObject({ status: 422, detail: 'El nombre es obligatorio', errors: [{ field: 'name' }] });
    });
  });

  describe('PrivacyApiService', () => {
    it('searches, exports and erases the data of one person', async () => {
      const api = TestBed.inject(PrivacyApiService);
      const search = api.search('ana@example.com');
      const searchRequest = http.expectOne('/api/v1/privacy/search');
      expect(searchRequest.request.body).toEqual({ query: 'ana@example.com' });
      searchRequest.flush({ entries: [] });
      await search;

      const exported = api.export('ana@example.com');
      http.expectOne('/api/v1/privacy/export').flush(new Blob(['{}']));
      expect(await exported).toBeInstanceOf(Blob);

      const erase = api.erase('ana@example.com');
      const eraseRequest = http.expectOne('/api/v1/privacy/erasure');
      expect(eraseRequest.request.body).toEqual({ query: 'ana@example.com', confirmed: true });
      eraseRequest.flush({ deleted: 1, anonymized: 0 });
      expect(await erase).toEqual({ deleted: 1, anonymized: 0 });
    });
  });

  describe('ApiKeysApiService', () => {
    it('lists, creates and revokes keys', async () => {
      const api = TestBed.inject(ApiKeysApiService);
      const list = api.list();
      http.expectOne('/api/v1/api-keys').flush({ items: [], maxActive: 5 });
      await list;
      const create = api.create('Integración');
      const createRequest = http.expectOne((r) => r.method === 'POST' && r.url === '/api/v1/api-keys');
      expect(createRequest.request.body).toEqual({ name: 'Integración' });
      createRequest.flush({ clientSecret: 's' });
      await create;
      const revoke = api.revoke(3);
      http.expectOne((r) => r.method === 'DELETE' && r.url === '/api/v1/api-keys/3').flush({ id: 3, active: false });
      await revoke;
    });
  });

  describe('PublicResultsService', () => {
    it('reads the public result and its counted votes with the id escaped', async () => {
      const api = TestBed.inject(PublicResultsService);
      const result = api.result('a b');
      http.expectOne('/api/v1/ballot-box/public/results/a%20b').flush({ processId: 1 });
      await result;
      const counted = api.countedVotes('a b');
      http.expectOne('/api/v1/ballot-box/public/results/a%20b/counted-votes').flush({ votes: [] });
      await counted;
    });
  });

  it('keeps errors that are not HTTP errors as they are', () => {
    const other = new Error('offline');
    expect(toApiError(other)).toBe(other);
  });
});
