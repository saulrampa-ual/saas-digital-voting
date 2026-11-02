import { describe, expect, it } from 'vitest';
import { ProcessStatus, ProcessSummary } from '../../../core/admin/process-api.service';
import { censusOverview, checkState, countByStatus, openNow, recentlyClosed, upcoming } from './home-dashboard';

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
});
