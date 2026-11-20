import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { adminProviders } from '../../../../testing/admin-test-doubles';
import { AdminActionsApiService } from '../../../core/admin/admin-actions-api.service';
import { NotificationsApiService } from '../../../core/admin/notifications-api.service';
import { ProcessApiService, ProcessSummary } from '../../../core/admin/process-api.service';
import { ResultsApiService } from '../../../core/admin/results-api.service';
import { ManagementRole } from '../../../core/auth/auth.service';
import { AdminHomePage } from './admin-home.page';

/** The page's protected members, reached from the test. */
type Home = {
  ionViewWillEnter(): void;
  organizationName(): string | null;
  orgLabel(): string;
  processes(): ProcessSummary[] | null;
  processesError(): boolean;
  openCards(): { participation: unknown }[] | null;
  failedEmails(): { failed: number }[];
  auditRows(): { state: string }[] | null;
  actions(): unknown[] | null;
  censuses(): unknown[] | null;
  censusesError(): boolean;
  organizations(): { accounts: number | null; admins: number | null }[] | null;
  organizationsTotal(): number;
  organizationsError(): boolean;
  attention(): { tone: string; textKey: string }[];
  warnings(): number;
  auditCounts(): { total: number; intact: number; broken: number; never: number };
  lastAudit(): unknown;
  activeOrganizations(): number;
  suspendedOrganizations(): number;
  accountsTotal(): number;
  singleAdminOrgs(): number;
  period(): { from: string | null; to: string | null };
  openTotals(): unknown;
  days(): unknown[];
  counts(): Record<string, number>;
  upcoming(): unknown[];
  tallied(): unknown[];
  pct(value: number | null): string;
  n(value: number): string;
  shortHash(hash: string | null): string;
  participationPct(p: { participation: number } | null): number;
  loadCensuses(): Promise<void>;
  loadProcesses(): Promise<void>;
};

const HOUR = 3_600_000;
const at = (hours: number) => new Date(Date.now() + hours * HOUR).toISOString();
const PROCESSES: ProcessSummary[] = [
  { id: 1, title: { es: 'Abierto' }, status: 'OPEN', opensAt: at(-2), closesAt: at(5) },
  { id: 2, title: { es: 'Borrador' }, status: 'DRAFT', opensAt: at(24), closesAt: at(48) },
  { id: 3, title: { es: 'Escrutado' }, status: 'TALLIED', opensAt: at(-48), closesAt: at(-24) },
  { id: 4, title: { es: 'Cerrado' }, status: 'CLOSED', opensAt: at(-30), closesAt: at(-3) },
  { id: 5, title: { es: 'Programado' }, status: 'SCHEDULED', opensAt: at(10), closesAt: at(20) },
];
const PARTICIPATION = {
  processId: 1, status: 'OPEN', opensAt: at(-2), closesAt: at(5), eligibleVoters: 10, votesCast: 4, pending: 6,
  participation: 40, byDay: [{ day: '2026-11-20', votes: 4, cumulative: 4 }],
};
const COUNTS = { pending: 0, delivered: 3, failed: 0, cancelled: 0 };
const check = (intact: boolean) => ({ processId: 3, intact, eventsChecked: 9, checkedAt: at(-1), finalHash: 'ab'.repeat(32), problems: [] });

/** Console home (IMP-44): one panel per role, built from the read APIs only. Synthetic data, no real backend. */
describe('AdminHomePage (inicio de la consola)', () => {
  let http: HttpTestingController;
  afterEach(() => vi.restoreAllMocks());

  function setUp(roles: ManagementRole[], overrides: { list?: () => Promise<ProcessSummary[]>; censuses?: () => Promise<unknown> } = {}) {
    const processApi = {
      list: vi.fn(overrides.list ?? (() => Promise.resolve(PROCESSES))),
      censuses: vi.fn(overrides.censuses ?? (() => Promise.resolve([
        { id: 1, name: 'Vacío', voterCount: 0, frozen: false },
        { id: 2, name: 'Congelado', voterCount: 30, frozen: true },
      ]))),
    };
    const resultsApi = {
      participation: vi.fn((id: number) => (id === 1 ? Promise.resolve(PARTICIPATION) : Promise.reject(new Error('x')))),
      lastCheck: vi.fn((id: number) => (id === 3 ? Promise.resolve(check(true)) : id === 4 ? Promise.resolve(null)
        : Promise.reject(new Error('x')))),
    };
    const notificationsApi = {
      summary: vi.fn((id: number) => (id === 4 ? Promise.reject(new Error('x')) : Promise.resolve({
        counts: { INVITATION: { ...COUNTS, failed: id === 1 ? 2 : 0 }, REMINDER: COUNTS, CLOSING: COUNTS },
      }))),
    };
    const actionsApi = { search: vi.fn().mockResolvedValue({ items: [{ action: 'CENSUS_CREATED' }], totalItems: 1 }) };
    TestBed.configureTestingModule({
      providers: [
        ...adminProviders(roles),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ProcessApiService, useValue: processApi },
        { provide: ResultsApiService, useValue: resultsApi },
        { provide: NotificationsApiService, useValue: notificationsApi },
        { provide: AdminActionsApiService, useValue: actionsApi },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    const page = TestBed.runInInjectionContext(() => new AdminHomePage()) as unknown as Home;
    return { page, processApi, actionsApi };
  }

  const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

  it('administrator: processes, open participation, failed emails, censuses and what needs attention', async () => {
    const { page } = setUp(['ADMIN']);
    page.ionViewWillEnter();
    http.expectOne('/api/v1/organizations/UAL').flush({ id: 'UAL', name: 'Universidad de Almería', status: 'ACTIVE' });
    await settle();
    await settle();

    expect(page.organizationName()).toBe('Universidad de Almería');
    expect(page.orgLabel()).toBe('Universidad de Almería');
    expect(page.processes()).toHaveLength(5);
    expect(page.counts()['DRAFT']).toBe(1);
    expect(page.openCards()).toHaveLength(1);
    expect(page.failedEmails()).toEqual([expect.objectContaining({ failed: 2 })]);
    expect(page.censuses()).toHaveLength(2);
    expect(page.period().from).not.toBeNull();
    expect(page.days().length).toBeGreaterThan(0);
    expect(page.openTotals()).toBeTruthy();
    expect(page.upcoming().length).toBeGreaterThan(0);
    expect(page.tallied()).toHaveLength(1);
    const keys = page.attention().map((item) => item.textKey);
    expect(keys).toEqual(expect.arrayContaining([
      'admin.home.attention.failedEmails', 'admin.home.attention.emptyCensus', 'admin.home.attention.drafts', 'admin.home.attention.frozen',
    ]));
    expect(page.warnings()).toBe(2);
  });

  it('shows «all good» when nothing needs attention and keeps the identifier if the organization cannot be read', async () => {
    const { page } = setUp(['ADMIN'], {
      list: () => Promise.resolve([]),
      censuses: () => Promise.resolve([{ id: 2, name: 'Lleno', voterCount: 3, frozen: false }]),
    });
    page.ionViewWillEnter();
    http.expectOne('/api/v1/organizations/UAL').flush({}, { status: 500, statusText: 'Error' });
    await settle();
    expect(page.organizationName()).toBeNull();
    expect(page.orgLabel()).toBe('UAL');
    expect(page.attention()[0].textKey).toBe('admin.home.attention.allGood');
    expect(page.warnings()).toBe(0);
  });

  it('marks the panels as failed when the lists cannot be read, and retries them', async () => {
    const { page, processApi } = setUp(['ADMIN'], {
      list: () => Promise.reject(new Error('down')),
      censuses: () => Promise.reject(new Error('down')),
    });
    page.ionViewWillEnter();
    http.expectOne('/api/v1/organizations/UAL').flush({ name: 'UAL' });
    await settle();
    expect(page.processesError()).toBe(true);
    expect(page.censusesError()).toBe(true);

    processApi.list.mockResolvedValue([]);
    processApi.censuses.mockResolvedValue([]);
    await page.loadProcesses();
    await page.loadCensuses();
    expect(page.processesError()).toBe(false);
    expect(page.censusesError()).toBe(false);
  });

  it('auditor: last chain checks of the closed processes and the latest actions', async () => {
    const { page, actionsApi } = setUp(['AUDITOR']);
    page.ionViewWillEnter();
    http.expectOne('/api/v1/organizations/UAL').flush({ name: 'UAL' });
    await settle();
    await settle();

    expect(actionsApi.search).toHaveBeenCalledWith({}, 0, 5);
    expect(page.actions()).toHaveLength(1);
    expect(page.censuses()).toBeNull();
    const states = page.auditRows()!.map((row) => row.state).sort();
    expect(states).toEqual(['INTACT', 'NEVER']);
    expect(page.auditCounts()).toEqual({ total: 2, intact: 1, broken: 0, never: 1 });
    expect(page.lastAudit()).not.toBeNull();
  });

  it('auditor: an empty list of actions when they cannot be read', async () => {
    const { page, actionsApi } = setUp(['AUDITOR']);
    actionsApi.search.mockRejectedValue(new Error('down'));
    page.ionViewWillEnter();
    http.expectOne('/api/v1/organizations/UAL').flush({ name: 'UAL' });
    await settle();
    expect(page.actions()).toEqual([]);
  });

  it('superadministrator: organizations with their accounts and administrators', async () => {
    const { page } = setUp(['SUPERADMIN']);
    page.ionViewWillEnter();
    http.expectOne('/api/v1/organizations/UAL').flush({ name: 'UAL' });
    http.expectOne((r) => r.url === '/api/v1/organizations').flush({
      items: [{ id: 'UAL', name: 'UAL', status: 'ACTIVE' }, { id: 'OLD', name: 'Antigua', status: 'SUSPENDED' }], totalItems: 2,
    });
    await settle();
    http.expectOne((r) => r.url === '/api/v1/organizations/UAL/users').flush({ items: [{ role: 'ADMIN' }, { role: 'AUDITOR' }], totalItems: 2 });
    http.expectOne((r) => r.url === '/api/v1/organizations/OLD/users').flush({}, { status: 500, statusText: 'Error' });
    await settle();

    expect(page.organizationsTotal()).toBe(2);
    expect(page.activeOrganizations()).toBe(1);
    expect(page.suspendedOrganizations()).toBe(1);
    expect(page.accountsTotal()).toBe(2);
    expect(page.singleAdminOrgs()).toBe(1);
    expect(page.organizations()![1]).toEqual(expect.objectContaining({ accounts: null, admins: null }));
  });

  it('superadministrator: the list of organizations fails', async () => {
    const { page } = setUp(['SUPERADMIN']);
    page.ionViewWillEnter();
    http.expectOne('/api/v1/organizations/UAL').flush({ name: 'UAL' });
    http.expectOne((r) => r.url === '/api/v1/organizations').flush({}, { status: 503, statusText: 'Unavailable' });
    await settle();
    expect(page.organizationsError()).toBe(true);
  });

  it('formats numbers, percentages and hashes for the panels', () => {
    const { page } = setUp(['ADMIN']);
    expect(page.pct(null)).toBe('—');
    expect(page.pct(12.345)).toBe('12,3');
    expect(page.n(1234)).toBe('1234');
    expect(page.shortHash(null)).toBe('—');
    expect(page.shortHash('abcdef0123456789')).toBe('abcd…6789');
    expect(page.participationPct(null)).toBe(0);
    expect(page.participationPct({ participation: 140 })).toBe(100);
    expect(page.participationPct({ participation: 40 })).toBe(40);
  });
});
