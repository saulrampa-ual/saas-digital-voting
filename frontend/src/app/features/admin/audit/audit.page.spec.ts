import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { adminProviders } from '../../../../testing/admin-test-doubles';
import { ProcessApiService } from '../../../core/admin/process-api.service';
import { ResultsApiService } from '../../../core/admin/results-api.service';
import { AuditPage } from './audit.page';

type Page = {
  ionViewWillEnter(): void;
  select(id: number): Promise<void>;
  go(delta: number): Promise<void>;
  verify(): Promise<void>;
  seq(value: number): string;
  short(hash: string): string;
  events(): { totalItems: number } | null;
  check(): { intact: boolean } | null;
  pageIndex(): number;
  error(): boolean;
  verifyError(): string | null;
  first(): number;
  last(): number;
};

const HASH = 'a'.repeat(64);
const EVENTS = {
  items: [{ sequence: 1, type: 'OPENED', occurredAt: '2026-11-19T10:00:00Z', payloadHash: HASH, previousHash: HASH, eventHash: HASH }],
  page: 0, size: 50, totalItems: 120, status: 'CLOSED', closesAt: '2026-11-19T18:00:00Z', finalHash: HASH,
};
const CHECK = { processId: 4, intact: true, eventsChecked: 120, checkedAt: '2026-11-20T10:00:00Z', finalHash: HASH, problems: [] };

/** H1 (RF-12): events of the hash chain of a process and its verification, for the auditor. */
describe('AuditPage (auditoría)', () => {
  let api: Record<'auditEvents' | 'lastCheck' | 'verifyChain', ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    api = {
      auditEvents: vi.fn().mockResolvedValue(EVENTS),
      lastCheck: vi.fn().mockResolvedValue(null),
      verifyChain: vi.fn().mockResolvedValue(CHECK),
    };
    TestBed.configureTestingModule({
      providers: [...adminProviders(['AUDITOR']), { provide: ResultsApiService, useValue: api },
        { provide: ProcessApiService, useValue: { list: vi.fn().mockResolvedValue([]) } }],
    });
  });

  function open(proceso?: string): Page {
    const fixture = TestBed.createComponent(AuditPage);
    if (proceso) fixture.componentRef.setInput('proceso', proceso);
    return fixture.componentInstance as unknown as Page;
  }
  const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

  it('does nothing without a process', async () => {
    const page = open();
    page.ionViewWillEnter();
    await page.verify();
    expect(api.auditEvents).not.toHaveBeenCalled();
    expect(page.first()).toBe(0);
  });

  it('loads the events and the last check, and pages through them', async () => {
    const page = open('4');
    page.ionViewWillEnter();
    await settle();
    expect(api.auditEvents).toHaveBeenCalledWith(4, 0, 50);
    expect(api.lastCheck).toHaveBeenCalledWith(4);
    expect(page.first()).toBe(1);
    expect(page.last()).toBe(50);
    await page.go(1);
    expect(api.auditEvents).toHaveBeenLastCalledWith(4, 1, 50);
    expect(api.lastCheck).toHaveBeenCalledTimes(1);
    await page.go(-3);
    expect(page.pageIndex()).toBe(0);
  });

  it('flags events that cannot be read', async () => {
    api.auditEvents.mockRejectedValue(new Error('down'));
    const page = open('4');
    page.ionViewWillEnter();
    await settle();
    expect(page.error()).toBe(true);
  });

  it('verifies the chain on demand, or reports that it could not', async () => {
    const page = open('4');
    await page.verify();
    expect(page.check()).toEqual(CHECK);
    api.verifyChain.mockRejectedValueOnce(new Error('down'));
    await page.verify();
    expect(page.verifyError()).toBe('common.loadError');
  });

  it('selects another process from the first page', async () => {
    const page = open('4');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await page.select(4);
    expect(navigate).toHaveBeenCalledWith([], { queryParams: { proceso: 4 }, replaceUrl: true });
    expect(page.pageIndex()).toBe(0);
  });

  it('formats sequence numbers and hashes', () => {
    const page = open();
    expect(page.seq(7)).toBe('0007');
    expect(page.short('0123456789abcdef')).toBe('0123…cdef');
  });
});
