import { CensusSummary, ProcessStatus, ProcessSummary } from '../../../core/admin/process-api.service';
import { ChainCheck } from '../../../core/admin/results-api.service';

/** Statuses shown in the summary strip of the administrator home, in lifecycle order. */
export const STRIP_STATUSES: readonly ProcessStatus[] = ['DRAFT', 'SCHEDULED', 'OPEN', 'TALLIED'];

/** Statuses whose ballot box can be verified by an auditor (the chain exists once the process has opened). */
export const VERIFIABLE_STATUSES: readonly ProcessStatus[] = ['CLOSED', 'TALLIED', 'ARCHIVED'];

export type CheckState = 'INTACT' | 'BROKEN' | 'NEVER';

export function countByStatus(processes: readonly ProcessSummary[]): Record<ProcessStatus, number> {
  const counts: Record<ProcessStatus, number> = { DRAFT: 0, SCHEDULED: 0, OPEN: 0, CLOSED: 0, TALLIED: 0, ARCHIVED: 0 };
  for (const process of processes) counts[process.status]++;
  return counts;
}

/** Open processes, the one closing first at the top. */
export function openNow(processes: readonly ProcessSummary[]): ProcessSummary[] {
  return processes.filter((p) => p.status === 'OPEN').sort((a, b) => time(a.closesAt) - time(b.closesAt));
}

/** Scheduled processes (next to open first), then drafts still to be scheduled. */
export function upcoming(processes: readonly ProcessSummary[], limit = 4): ProcessSummary[] {
  const byOpening = (a: ProcessSummary, b: ProcessSummary) => time(a.opensAt) - time(b.opensAt);
  const scheduled = processes.filter((p) => p.status === 'SCHEDULED').sort(byOpening);
  const drafts = processes.filter((p) => p.status === 'DRAFT').sort(byOpening);
  return [...scheduled, ...drafts].slice(0, limit);
}

/** Processes that ended, most recently closed first. */
export function recentlyClosed(processes: readonly ProcessSummary[], statuses: readonly ProcessStatus[], limit = 3): ProcessSummary[] {
  return processes.filter((p) => statuses.includes(p.status)).sort((a, b) => time(b.closesAt) - time(a.closesAt)).slice(0, limit);
}

export interface CensusOverview {
  total: number;
  people: number;
  editable: number;
  frozen: number;
  /** Editable censuses without people: a process cannot be scheduled with them. */
  empty: CensusSummary[];
}

export function censusOverview(censuses: readonly CensusSummary[]): CensusOverview {
  return {
    total: censuses.length,
    people: censuses.reduce((sum, c) => sum + c.voterCount, 0),
    editable: censuses.filter((c) => !c.frozen).length,
    frozen: censuses.filter((c) => c.frozen).length,
    empty: censuses.filter((c) => !c.frozen && c.voterCount === 0),
  };
}

export function checkState(check: ChainCheck | null): CheckState {
  if (!check) return 'NEVER';
  return check.intact ? 'INTACT' : 'BROKEN';
}

function time(value: string): number {
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? 0 : parsed;
}

/** Minimal shape of RF-14 participation used by the home (counts of people only, per day, D-18). */
export interface ParticipationCounts {
  eligibleVoters: number;
  votesCast: number;
  byDay: readonly { day: string; votes: number; cumulative: number }[];
}

export interface ParticipationTotals {
  votes: number;
  eligible: number;
  /** Votes over the summed censuses, 0–100; null when there is no census to divide by. */
  pct: number | null;
}

/** Totals over several processes: people who voted over people in their censuses (not an average of percentages). */
export function participationTotals(items: readonly ParticipationCounts[]): ParticipationTotals {
  const votes = items.reduce((sum, p) => sum + p.votesCast, 0);
  const eligible = items.reduce((sum, p) => sum + p.eligibleVoters, 0);
  return { votes, eligible, pct: eligible > 0 ? (votes * 100) / eligible : null };
}

export interface DayPoint {
  /** Calendar day `YYYY-MM-DD` (never a time, D-18). */
  day: string;
  cumulative: number;
  /** Cumulative votes over the summed censuses, 0–100. */
  pct: number;
}

/**
 * Cumulative votes per day across processes. A process without votes on a day keeps its previous cumulative value, so
 * every day adds up all processes. Days only (D-18): there is no finer breakdown to show.
 */
export function cumulativeByDay(items: readonly ParticipationCounts[]): DayPoint[] {
  const eligible = items.reduce((sum, p) => sum + p.eligibleVoters, 0);
  const days = [...new Set(items.flatMap((p) => p.byDay.map((d) => d.day)))].sort();
  return days.map((day) => {
    const cumulative = items.reduce((sum, p) => {
      const upTo = p.byDay.filter((d) => d.day <= day);
      return sum + (upTo.length ? Math.max(...upTo.map((d) => d.cumulative)) : 0);
    }, 0);
    return { day, cumulative, pct: eligible > 0 ? (cumulative * 100) / eligible : 0 };
  });
}

/** The most recent chain verification among the given ones, or null if none was ever run. */
export function latestCheck<T extends { checkedAt: string } | null>(checks: readonly T[]): NonNullable<T> | null {
  const done = checks.filter((c): c is NonNullable<T> => c !== null);
  if (done.length === 0) return null;
  return done.reduce((a, b) => (time(b.checkedAt) > time(a.checkedAt) ? b : a));
}

/** Management accounts with the ADMIN role; with only one, the organization depends on a single person. */
export function adminCount(users: readonly { role: string }[]): number {
  return users.filter((u) => u.role === 'ADMIN').length;
}
