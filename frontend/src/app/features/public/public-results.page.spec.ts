import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { adminProviders } from '../../../testing/admin-test-doubles';
import { ApiError } from '../../core/admin/api-error';
import { PublicResultsService } from '../../core/results/public-results.service';
import { PublicResultsPage } from './public-results.page';

type Page = {
  ionViewWillEnter(): Promise<void>;
  result(): unknown;
  loading(): boolean;
  notFound(): boolean;
  failed(): boolean;
  verification(): string;
};

const RESULT = {
  processId: 9, status: 'TALLIED', talliedAt: '2026-11-19T10:00:00Z', voteType: 'SINGLE_CHOICE', question: { es: '¿?' },
  options: [{ optionId: 1, position: 1, text: { es: 'A' }, votes: 1, percentage: 100 }], blankVoteEnabled: false,
  blankVotes: 0, blankPercentage: 0, totalVotes: 1, eligibleVoters: 2, participation: 50, finalHash: 'f', resultHash: 'r',
};

/** V8 (RF-15, D-16): result published by link, with its independent recomputation in the browser. */
describe('PublicResultsPage (resultado público)', () => {
  let api: { result: ReturnType<typeof vi.fn>; countedVotes: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    api = {
      result: vi.fn().mockResolvedValue(RESULT),
      countedVotes: vi.fn().mockResolvedValue({ processId: 9, finalHash: 'f', resultHash: 'r', eligibleVoters: 2, optionIds: [1],
        votes: [{ voteId: 'v1', optionId: 1 }] }),
    };
    TestBed.configureTestingModule({ providers: [...adminProviders([]), { provide: PublicResultsService, useValue: api }] });
  });

  function open(): Page {
    const fixture = TestBed.createComponent(PublicResultsPage);
    fixture.componentRef.setInput('publicId', 'pub-1');
    return fixture.componentInstance as unknown as Page;
  }

  it('shows the result and recomputes it', async () => {
    const page = open();
    await page.ionViewWillEnter();
    expect(page.result()).toEqual(RESULT);
    expect(page.loading()).toBe(false);
    await vi.waitFor(() => expect(page.verification()).toBe('mismatch'));
  });

  it('reports a recomputation that could not be done', async () => {
    api.countedVotes.mockRejectedValue(new Error('down'));
    const page = open();
    await page.ionViewWillEnter();
    await vi.waitFor(() => expect(page.verification()).toBe('error'));
  });

  it('tells an unknown link (404 or 400) from a service failure', async () => {
    const page = open();
    api.result.mockRejectedValueOnce(new ApiError(404, null, []));
    await page.ionViewWillEnter();
    expect(page.notFound()).toBe(true);
    api.result.mockRejectedValueOnce(new ApiError(400, null, []));
    await page.ionViewWillEnter();
    expect(page.notFound()).toBe(true);
    api.result.mockRejectedValueOnce(new ApiError(503, null, []));
    await page.ionViewWillEnter();
    expect(page.notFound()).toBe(false);
    expect(page.failed()).toBe(true);
  });
});
