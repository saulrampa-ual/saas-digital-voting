import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { adminProviders } from '../../../../testing/admin-test-doubles';
import { ApiError } from '../../../core/admin/api-error';
import { Process, ProcessApiService } from '../../../core/admin/process-api.service';
import { ManagementRole } from '../../../core/auth/auth.service';
import { ProcessDetailPage } from './process-detail.page';

type Page = {
  ionViewWillEnter(): void;
  process(): Process | null;
  census(): { name: string } | null;
  loading(): boolean;
  loadError(): boolean;
  links(): { path: string }[];
  showChecks(): boolean;
  hasActions(): boolean;
  currentIndex(): number;
  checks(): { key: string; ok: boolean }[];
  dateOf(status: string): string | null;
  confirmButtons(): { role: string }[];
  onConfirmDismiss(role: string | undefined): Promise<void>;
  archive(): Promise<void>;
  notice(): string | null;
  actionError(): string | null;
  issues(): unknown[];
  busy(): boolean;
};

const DAY = 86_400_000;
const process = (over: Partial<Process> = {}): Process => ({
  id: 3, status: 'DRAFT', censusId: 7, title: { es: 'Elección', en: 'Election' }, description: { es: 'Texto' },
  opensAt: new Date(Date.now() + DAY).toISOString(), closesAt: new Date(Date.now() + 2 * DAY).toISOString(),
  defaultLanguage: 'es', resultsVisibility: 'CENSUS_ONLY', voterAuthentication: 'EMAIL_CODE',
  ballot: { voteType: 'SINGLE_CHOICE', question: { es: '¿?', en: '?' }, blankVoteEnabled: true,
    options: [{ id: 1, position: 1, text: { es: 'A', en: 'A' } }] },
  ...over,
});

/** A4 (RF-07): one process, its status line, the pre-scheduling checks and the manual actions. */
describe('ProcessDetailPage (detalle del proceso)', () => {
  let api: Record<'get' | 'history' | 'censuses' | 'schedule' | 'archive', ReturnType<typeof vi.fn>>;

  function open(roles: ManagementRole[] = ['ADMIN']): Page {
    TestBed.configureTestingModule({ providers: [...adminProviders(roles), { provide: ProcessApiService, useValue: api }] });
    const fixture = TestBed.createComponent(ProcessDetailPage);
    fixture.componentRef.setInput('id', '3');
    return fixture.componentInstance as unknown as Page;
  }
  const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

  beforeEach(() => {
    api = {
      get: vi.fn().mockResolvedValue(process()),
      history: vi.fn().mockResolvedValue([{ from: 'DRAFT', to: 'SCHEDULED', changedAt: '2026-11-19T10:00:00Z', changedBy: 'Ana' }]),
      censuses: vi.fn().mockResolvedValue([{ id: 7, name: 'Censo', voterCount: 0, frozen: false }]),
      schedule: vi.fn().mockResolvedValue(process({ status: 'SCHEDULED' })),
      archive: vi.fn().mockResolvedValue(process({ status: 'ARCHIVED' })),
    };
  });

  it('administrator: loads a draft, its census and the checks before scheduling', async () => {
    const page = open();
    page.ionViewWillEnter();
    await settle();
    expect(page.loading()).toBe(false);
    expect(page.census()?.name).toBe('Censo');
    expect(page.showChecks()).toBe(true);
    expect(page.hasActions()).toBe(true);
    expect(page.links()).toEqual([]);
    expect(page.currentIndex()).toBe(0);
    const checks = Object.fromEntries(page.checks().map((c) => [c.key, c.ok]));
    expect(checks).toEqual({ texts: false, calendar: true, census: false, ballot: true });
    expect(page.dateOf('OPEN')).toBe(page.process()!.opensAt);
    expect(page.dateOf('CLOSED')).toBe(page.process()!.closesAt);
    expect(page.dateOf('SCHEDULED')).toBe('2026-11-19T10:00:00Z');
    expect(page.dateOf('TALLIED')).toBeNull();
    expect(page.confirmButtons().map((b) => b.role)).toEqual(['cancel', 'confirm']);
  });

  it('auditor: links to the results and the audit of a closed process, without reading censuses', async () => {
    api.get.mockResolvedValue(process({ status: 'TALLIED' }));
    const page = open(['AUDITOR']);
    expect(page.dateOf('OPEN')).toBeNull();
    expect(page.checks()).toEqual([]);
    page.ionViewWillEnter();
    await settle();
    expect(api.censuses).not.toHaveBeenCalled();
    expect(page.links().map((l) => l.path)).toEqual(['/admin/resultados', '/admin/auditoria']);
    expect(page.showChecks()).toBe(false);
  });

  it('flags a process that cannot be read', async () => {
    api.get.mockRejectedValue(new Error('down'));
    const page = open();
    page.ionViewWillEnter();
    await settle();
    expect(page.loadError()).toBe(true);
  });

  it('schedules only when the dialog is confirmed', async () => {
    const page = open();
    await page.onConfirmDismiss('cancel');
    expect(api.schedule).not.toHaveBeenCalled();
    await page.onConfirmDismiss('confirm');
    expect(api.schedule).toHaveBeenCalledWith(3);
    expect(page.process()?.status).toBe('SCHEDULED');
    expect(page.notice()).toBe('admin.process.scheduled');
    expect(page.busy()).toBe(false);
  });

  it('shows the reasons of a refused schedule, or a generic error', async () => {
    const page = open();
    api.schedule.mockRejectedValueOnce(new ApiError(422, 'No se puede programar', [{ field: 'opensAt', message: 'Pasada' }]));
    await page.onConfirmDismiss('confirm');
    expect(page.actionError()).toBe('No se puede programar');
    expect(page.issues()).toHaveLength(1);
    api.archive.mockRejectedValueOnce(new Error('down'));
    await page.archive();
    expect(page.actionError()).toBe('common.loadError');
  });

  it('archives a tallied process', async () => {
    const page = open();
    await page.archive();
    expect(page.process()?.status).toBe('ARCHIVED');
    expect(page.notice()).toBe('admin.process.archived');
  });
});
