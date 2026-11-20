import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { adminProviders } from '../../../../testing/admin-test-doubles';
import { NotificationsApiService } from '../../../core/admin/notifications-api.service';
import { ProcessApiService } from '../../../core/admin/process-api.service';
import { Participation, ResultsApiService } from '../../../core/admin/results-api.service';
import { ParticipationPage } from './participation.page';

type Page = {
  ionViewWillEnter(): void;
  ionViewWillLeave(): void;
  ngOnDestroy(): void;
  select(id: number): Promise<void>;
  data(): Participation | null;
  error(): boolean;
  secondsAgo(): number;
  chartPoints(): { pct: number }[];
  pct(d: Participation): string;
  share(part: number, total: number): string;
  ratio(part: number, total: number): number;
  n(value: number): string;
};

const DATA: Participation = {
  processId: 4, status: 'OPEN', opensAt: '2026-11-19T10:00:00Z', closesAt: '2026-11-21T10:00:00Z', eligibleVoters: 8,
  votesCast: 2, pending: 6, participation: 25, byDay: [{ day: '2026-11-19', votes: 1, cumulative: 1 }, { day: '2026-11-20', votes: 1, cumulative: 2 }],
};
const COUNTS = { pending: 0, delivered: 0, failed: 0, cancelled: 0 };

/** A5 (RF-14): participation of a process, as counts of people by day (D-18), refreshed while it is open. */
describe('ParticipationPage (participación)', () => {
  let api: { participation: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    api = { participation: vi.fn().mockResolvedValue(DATA) };
    TestBed.configureTestingModule({
      providers: [...adminProviders(), { provide: ResultsApiService, useValue: api },
        { provide: ProcessApiService, useValue: { list: vi.fn().mockResolvedValue([]) } },
        { provide: NotificationsApiService, useValue: { summary: vi.fn().mockResolvedValue({
          processId: 4, status: 'OPEN', opensAt: DATA.opensAt, closesAt: DATA.closesAt,
          counts: { INVITATION: COUNTS, REMINDER: COUNTS, CLOSING: COUNTS }, failed: [], reminders: [], maxReminders: 2 }) } }],
    });
  });
  afterEach(() => vi.useRealTimers());

  function open(proceso?: string): Page {
    const fixture = TestBed.createComponent(ParticipationPage);
    if (proceso) fixture.componentRef.setInput('proceso', proceso);
    return fixture.componentInstance as unknown as Page;
  }

  it('loads the participation, draws the chart and refreshes it while the process is open', async () => {
    vi.useFakeTimers();
    const page = open('4');
    page.ionViewWillEnter();
    await vi.advanceTimersByTimeAsync(0);
    expect(page.data()).toEqual(DATA);
    expect(page.chartPoints().map((p) => p.pct)).toEqual([12.5, 25]);
    await vi.advanceTimersByTimeAsync(15_000);
    expect(api.participation).toHaveBeenCalledTimes(2);
    expect(page.secondsAgo()).toBeGreaterThanOrEqual(0);
    page.ionViewWillLeave();
    page.ngOnDestroy();
  });

  it('flags a participation that cannot be read and draws nothing without data', async () => {
    api.participation.mockRejectedValue(new Error('down'));
    const page = open('4');
    expect(page.chartPoints()).toEqual([]);
    page.ionViewWillEnter();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(page.error()).toBe(true);
    page.ngOnDestroy();
  });

  it('does not ask without a process, and changes it through the address bar', async () => {
    const page = open();
    page.ionViewWillEnter();
    expect(api.participation).not.toHaveBeenCalled();
    page.ngOnDestroy();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await page.select(4);
    expect(navigate).toHaveBeenCalled();
  });

  it('formats percentages and shares, with an empty census as zero', () => {
    const page = open();
    expect(page.pct(DATA)).toBe('25');
    expect(page.share(1, 3)).toBe('33,3');
    expect(page.share(1, 0)).toBe('0');
    expect(page.ratio(5, 4)).toBe(100);
    expect(page.ratio(1, 0)).toBe(0);
    expect(page.n(12)).toBe('12');
  });
});
