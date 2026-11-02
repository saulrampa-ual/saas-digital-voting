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
