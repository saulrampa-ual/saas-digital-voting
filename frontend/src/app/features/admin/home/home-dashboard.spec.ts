import { describe, expect, it } from 'vitest';
import { ProcessStatus, ProcessSummary } from '../../../core/admin/process-api.service';
import {
  censusOverview, checkState, countByStatus, cumulativeByDay, latestCheck, openNow, participationTotals, recentlyClosed, adminCount,
  upcoming,
} from './home-dashboard';

let next = 1;
function process(status: ProcessStatus, opensAt: string, closesAt: string): ProcessSummary {
  return { id: next++, title: { es: `Proceso ${next}` }, status, opensAt, closesAt };
}

describe('home dashboard', () => {
  const draft = process('DRAFT', '2026-12-10T09:00:00Z', '2026-12-11T09:00:00Z');
  const laterScheduled = process('SCHEDULED', '2026-12-01T09:00:00Z', '2026-12-02T09:00:00Z');
  const soonScheduled = process('SCHEDULED', '2026-11-20T09:00:00Z', '2026-11-21T09:00:00Z');
  const openLate = process('OPEN', '2026-11-01T09:00:00Z', '2026-11-30T09:00:00Z');
  const openSoon = process('OPEN', '2026-11-01T09:00:00Z', '2026-11-05T09:00:00Z');
  const oldTally = process('TALLIED', '2026-09-01T09:00:00Z', '2026-09-02T09:00:00Z');
  const newTally = process('TALLIED', '2026-10-01T09:00:00Z', '2026-10-02T09:00:00Z');
  const archived = process('ARCHIVED', '2026-08-01T09:00:00Z', '2026-08-02T09:00:00Z');
  const all = [draft, laterScheduled, openLate, oldTally, soonScheduled, openSoon, newTally, archived];

  it('counts processes by status, including the ones not in the strip', () => {
    expect(countByStatus(all)).toEqual({ DRAFT: 1, SCHEDULED: 2, OPEN: 2, CLOSED: 0, TALLIED: 2, ARCHIVED: 1 });
  });

  it('lists open processes by closing time', () => {
    expect(openNow(all).map((p) => p.id)).toEqual([openSoon.id, openLate.id]);
  });

  it('lists scheduled processes by opening time before drafts', () => {
    expect(upcoming(all).map((p) => p.id)).toEqual([soonScheduled.id, laterScheduled.id, draft.id]);
    expect(upcoming(all, 1)).toHaveLength(1);
  });

  it('lists finished processes of the given statuses, most recent first', () => {
    expect(recentlyClosed(all, ['TALLIED']).map((p) => p.id)).toEqual([newTally.id, oldTally.id]);
    expect(recentlyClosed(all, ['TALLIED', 'ARCHIVED'], 2).map((p) => p.id)).toEqual([newTally.id, oldTally.id]);
  });

  it('summarizes censuses and flags editable empty ones only', () => {
    const overview = censusOverview([
      { id: 1, name: 'A', voterCount: 10, frozen: true },
      { id: 2, name: 'B', voterCount: 0, frozen: false },
      { id: 3, name: 'C', voterCount: 5, frozen: false },
      { id: 4, name: 'D', voterCount: 0, frozen: true },
    ]);
    expect(overview).toMatchObject({ total: 4, people: 15, editable: 2, frozen: 2 });
    expect(overview.empty.map((c) => c.id)).toEqual([2]);
  });

  it('maps the last chain check to a state', () => {
    const base = { processId: 1, eventsChecked: 3, checkedAt: '2026-11-01T10:00:00Z', finalHash: null, problems: [] };
    expect(checkState(null)).toBe('NEVER');
    expect(checkState({ ...base, intact: true })).toBe('INTACT');
    expect(checkState({ ...base, intact: false })).toBe('BROKEN');
  });

  it('adds people who voted over people in the censuses, not an average of percentages', () => {
    const totals = participationTotals([
      { eligibleVoters: 100, votesCast: 50, byDay: [] },
      { eligibleVoters: 300, votesCast: 30, byDay: [] },
    ]);
    expect(totals).toEqual({ votes: 80, eligible: 400, pct: 20 });
    expect(participationTotals([]).pct).toBeNull();
  });

  it('accumulates votes per day across processes, carrying the last value of each process', () => {
    const points = cumulativeByDay([
      { eligibleVoters: 10, votesCast: 4, byDay: [{ day: '2026-11-01', votes: 1, cumulative: 1 }, { day: '2026-11-03', votes: 3, cumulative: 4 }] },
      { eligibleVoters: 10, votesCast: 2, byDay: [{ day: '2026-11-02', votes: 2, cumulative: 2 }] },
    ]);
    expect(points).toEqual([
      { day: '2026-11-01', cumulative: 1, pct: 5 },
      { day: '2026-11-02', cumulative: 3, pct: 15 },
      { day: '2026-11-03', cumulative: 6, pct: 30 },
    ]);
  });

  it('picks the most recent chain check and counts administrators', () => {
    const a = { checkedAt: '2026-11-01T10:00:00Z' };
    const b = { checkedAt: '2026-11-02T08:00:00Z' };
    expect(latestCheck([null, a, b, null])).toBe(b);
    expect(latestCheck([null])).toBeNull();
    expect(adminCount([{ role: 'ADMIN' }, { role: 'AUDITOR' }])).toBe(1);
    expect(adminCount([{ role: 'ADMIN' }, { role: 'ADMIN' }, { role: 'CENSUS_MANAGER' }])).toBe(2);
  });
});
