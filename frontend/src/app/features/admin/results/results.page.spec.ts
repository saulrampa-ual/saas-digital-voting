import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { adminProviders, stubDownloads } from '../../../../testing/admin-test-doubles';
import { ProcessApiService } from '../../../core/admin/process-api.service';
import { Result, ResultsApiService } from '../../../core/admin/results-api.service';
import { ResultsPage } from './results.page';

type Page = {
  ionViewWillEnter(): void;
  ionViewWillLeave(): void;
  ngOnDestroy(): void;
  select(id: number): Promise<void>;
  exportCsv(): Promise<void>;
  downloadMinutes(): Promise<void>;
  copy(text: string): Promise<void>;
  n(value: number): string;
  pct(value: number): string;
  process(): { resultsPublicId?: string | null } | null;
  result(): Result | null;
  verification(): string;
  loading(): boolean;
  error(): boolean;
  exportError(): string | null;
  minutesError(): string | null;
  minutesHash(): string | null;
  copied(): boolean;
  publicLink(): string | null;
  canExport: boolean;
};

const RESULT = {
  processId: 9, status: 'TALLIED', talliedAt: '2026-11-19T10:00:00Z', voteType: 'SINGLE_CHOICE', question: { es: '¿?' },
  options: [{ optionId: 1, position: 1, text: { es: 'A' }, votes: 1, percentage: 100 }], blankVoteEnabled: false,
  blankVotes: 0, blankPercentage: 0, totalVotes: 1, eligibleVoters: 2, participation: 50, finalHash: 'f', resultHash: 'r',
} as Result;

const PROCESS = {
  id: 9, status: 'TALLIED', resultsPublicId: 'pub-1', censusId: 1, title: { es: 'Elección' }, description: {},
  opensAt: '2026-11-18T10:00:00Z', closesAt: '2026-11-19T10:00:00Z', defaultLanguage: 'es', resultsVisibility: 'PUBLIC_LINK',
  voterAuthentication: 'EMAIL_CODE', ballot: { voteType: 'SINGLE_CHOICE', question: { es: '¿?' }, blankVoteEnabled: false, options: [] },
};

/** F1–F4 (RF-13, RF-15, RF-16): result of a closed process, its check, the CSV, the minutes and the public link. */
describe('ResultsPage (resultados)', () => {
  let api: Record<'result' | 'countedVotes' | 'exportCsv' | 'minutes', ReturnType<typeof vi.fn>>;
  let processes: { get: ReturnType<typeof vi.fn>; list: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    stubDownloads();
    api = {
      result: vi.fn().mockResolvedValue(RESULT),
      countedVotes: vi.fn().mockResolvedValue({ processId: 9, finalHash: 'f', resultHash: 'r', eligibleVoters: 2, optionIds: [1],
        votes: [{ voteId: 'v1', optionId: 1 }] }),
      exportCsv: vi.fn().mockResolvedValue(new Blob(['a'])),
      minutes: vi.fn().mockResolvedValue({ file: new Blob(['%PDF']), sha256: 'abc' }),
    };
    processes = { get: vi.fn().mockResolvedValue(PROCESS), list: vi.fn().mockResolvedValue([]) } as typeof processes;
    TestBed.configureTestingModule({
      providers: [
        ...adminProviders(['ADMIN']),
        { provide: ResultsApiService, useValue: api },
        { provide: ProcessApiService, useValue: processes },
      ],
    });
  });
  afterEach(() => vi.useRealTimers());

  function open(proceso?: string): Page {
    const fixture = TestBed.createComponent(ResultsPage);
    if (proceso) fixture.componentRef.setInput('proceso', proceso);
    return fixture.componentInstance as unknown as Page;
  }
  const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

  it('does nothing until a process is chosen', async () => {
    const page = open();
    page.ionViewWillEnter();
    await page.exportCsv();
    await page.downloadMinutes();
    expect(api.result).not.toHaveBeenCalled();
    expect(page.canExport).toBe(true);
  });

  it('loads the result and recomputes it (the hash does not match the synthetic one)', async () => {
    const page = open('9');
    page.ionViewWillEnter();
    await settle();
    await settle();
    expect(page.result()).toEqual(RESULT);
    expect(page.publicLink()).toContain('/resultados/pub-1');
    await vi.waitFor(() => expect(page.verification()).toBe('mismatch'));
    expect(page.loading()).toBe(false);
  });

  it('reports a verification error when the counted votes cannot be read', async () => {
    api.countedVotes.mockRejectedValue(new Error('down'));
    const page = open('9');
    page.ionViewWillEnter();
    await settle();
    await settle();
    expect(page.verification()).toBe('error');
  });

  it('waits for the tally and asks again a few seconds later', async () => {
    vi.useFakeTimers();
    api.result.mockResolvedValueOnce(null).mockResolvedValue(RESULT);
    const page = open('9');
    page.ionViewWillEnter();
    await vi.advanceTimersByTimeAsync(0);
    expect(page.result()).toBeNull();
    await vi.advanceTimersByTimeAsync(10_000);
    expect(api.result).toHaveBeenCalledTimes(2);
    page.ionViewWillLeave();
    page.ngOnDestroy();
  });

  it('shows an error when the process cannot be read', async () => {
    processes.get.mockRejectedValue(new Error('down'));
    const page = open('9');
    page.ionViewWillEnter();
    await settle();
    expect(page.error()).toBe(true);
  });

  it('changes the selected process in the address bar', async () => {
    const page = open('9');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await page.select(9);
    expect(navigate).toHaveBeenCalledWith([], { queryParams: { proceso: 9 }, replaceUrl: true });
  });

  it('downloads the CSV and the minutes, keeping the SHA-256 of the minutes', async () => {
    const page = open('9');
    await page.exportCsv();
    expect(api.exportCsv).toHaveBeenCalledWith(9, 'es');
    expect(page.exportError()).toBeNull();
    await page.downloadMinutes();
    expect(page.minutesHash()).toBe('abc');
    expect(page.minutesError()).toBeNull();
  });

  it('shows the errors of the CSV and of the minutes', async () => {
    api.exportCsv.mockRejectedValue(new Error('down'));
    api.minutes.mockRejectedValue(new Error('down'));
    const page = open('9');
    await page.exportCsv();
    await page.downloadMinutes();
    expect(page.exportError()).toBe('admin.results.exportError');
    expect(page.minutesError()).toBe('admin.results.minutesError');
  });

  it('copies the public link, and stays quiet when the clipboard is not allowed', async () => {
    const page = open('9');
    await page.copy('https://example.com');
    expect(page.copied()).toBe(true);
    stubDownloads('denied');
    const other = open('9');
    await other.copy('https://example.com');
    expect(other.copied()).toBe(false);
  });

  it('formats counts and percentages', () => {
    const page = open('9');
    expect(page.n(1500)).toBe('1500');
    expect(page.pct(33.333)).toBe('33,33');
  });
});
