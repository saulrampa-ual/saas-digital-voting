import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { adminProviders } from '../../../testing/admin-test-doubles';
import { ProcessApiService, ProcessSummary } from '../../core/admin/process-api.service';
import { VoterApiError, VoterApiService } from '../../core/voter/voter-api.service';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { ReceiptCheckPage } from '../voter/receipt-check.page';
import { AdminNavService } from './admin-nav.service';
import { OrganizationsPage } from './organizations/organizations.page';
import { ProcessPickerComponent } from './process-picker/process-picker.component';
import { ProcessesPage } from './processes/processes.page';

const settle = () => new Promise((resolve) => setTimeout(resolve, 0));
const summary = (id: number, status: ProcessSummary['status']): ProcessSummary =>
  ({ id, title: { es: `Proceso ${id}` }, status, opensAt: '2026-11-19T10:00:00Z', closesAt: '2026-11-20T10:00:00Z' });

/** Small pages and parts of the console and of the voter, with their services replaced by doubles. */
describe('ProcessesPage (A1)', () => {
  it('lists the processes and flags a list that cannot be read', async () => {
    const api = { list: vi.fn().mockResolvedValue([summary(1, 'OPEN')]) };
    TestBed.configureTestingModule({ providers: [...adminProviders(), { provide: ProcessApiService, useValue: api }] });
    const fixture = TestBed.createComponent(ProcessesPage);
    const page = fixture.componentInstance as unknown as {
      ionViewWillEnter(): void; processes(): unknown[]; error(): boolean; canManage: boolean };
    page.ionViewWillEnter();
    await settle();
    expect(page.processes()).toHaveLength(1);
    expect(page.canManage).toBe(true);
    fixture.detectChanges();
    await fixture.whenStable();
    api.list.mockRejectedValue(new Error('down'));
    page.ionViewWillEnter();
    await settle();
    expect(page.error()).toBe(true);
  });
});

describe('OrganizationsPage (E2)', () => {
  it('lists the organizations, or shows an error', async () => {
    TestBed.configureTestingModule({ providers: [...adminProviders(['SUPERADMIN']), provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    type Page = { organizations(): unknown[]; loading(): boolean; error(): boolean };
    const fixture = TestBed.createComponent(OrganizationsPage);
    const page = fixture.componentInstance as unknown as Page;
    http.expectOne('/api/v1/organizations').flush({ items: [{ id: 'UAL', name: 'UAL', status: 'ACTIVE' }], totalItems: 1 });
    expect(page.organizations()).toHaveLength(1);
    expect(page.loading()).toBe(false);
    fixture.detectChanges();
    await fixture.whenStable();
    const failing = TestBed.runInInjectionContext(() => new OrganizationsPage()) as unknown as Page;
    http.expectOne('/api/v1/organizations').flush({}, { status: 500, statusText: 'Error' });
    expect(failing.error()).toBe(true);
    http.verify();
  });
});

describe('ProcessPickerComponent', () => {
  function open(list: ProcessSummary[], selected: number | null) {
    TestBed.configureTestingModule({
      providers: [...adminProviders(), { provide: ProcessApiService, useValue: { list: vi.fn().mockResolvedValue(list) } }],
    });
    const fixture = TestBed.createComponent(ProcessPickerComponent);
    fixture.componentRef.setInput('statuses', ['CLOSED', 'TALLIED']);
    fixture.componentRef.setInput('selected', selected);
    const emitted: number[] = [];
    fixture.componentInstance.selectedChange.subscribe((id) => emitted.push(id));
    return { picker: fixture.componentInstance as unknown as { ngOnInit(): Promise<void>; current(): ProcessSummary | null; options(): unknown[] }, emitted };
  }

  it('offers only the processes in the given statuses and selects the first when none is chosen', async () => {
    const { picker, emitted } = open([summary(1, 'OPEN'), summary(2, 'CLOSED'), summary(3, 'TALLIED')], null);
    await picker.ngOnInit();
    expect(picker.options()).toHaveLength(2);
    expect(emitted).toContain(2);
    expect(picker.current()).toBeNull();
  });

  it('keeps a valid selection', async () => {
    const { picker, emitted } = open([summary(2, 'CLOSED'), summary(3, 'TALLIED')], 3);
    await picker.ngOnInit();
    expect(emitted).toEqual([]);
    expect(picker.current()?.id).toBe(3);
  });
});

describe('ReceiptCheckPage (B5)', () => {
  function open(present: () => Promise<boolean>) {
    TestBed.configureTestingModule({
      providers: [...adminProviders([]), { provide: VoterApiService, useValue: { receiptPresent: vi.fn(present) } }],
    });
    const fixture = TestBed.createComponent(ReceiptCheckPage);
    fixture.componentRef.setInput('processId', '12');
    return fixture.componentInstance as unknown as {
      code: { set(v: string): void }; check(): Promise<void>; result(): string | null; formatError(): boolean };
  }
  const CODE = 'A'.repeat(64);

  it('rejects a code that is not 64 hexadecimal characters', async () => {
    const page = open(() => Promise.resolve(true));
    page.code.set('1234');
    await page.check();
    expect(page.formatError()).toBe(true);
  });

  it('says whether the receipt is in the ballot box', async () => {
    const present = open(() => Promise.resolve(true));
    present.code.set(CODE);
    await present.check();
    expect(present.result()).toBe('present');
  });

  it('tells an absent receipt, an unknown process and a failure apart', async () => {
    let answer: () => Promise<boolean> = () => Promise.resolve(false);
    const page = open(() => answer());
    page.code.set(CODE);
    await page.check();
    expect(page.result()).toBe('absent');
    answer = () => Promise.reject(new VoterApiError(404, {}));
    await page.check();
    expect(page.result()).toBe('unknownProcess');
    answer = () => Promise.reject(new Error('offline'));
    await page.check();
    expect(page.result()).toBe('error');
  });
});

describe('StatusBadgeComponent and AdminNavService', () => {
  it('gives each status its tone', () => {
    TestBed.configureTestingModule({ providers: adminProviders() });
    const fixture = TestBed.createComponent(StatusBadgeComponent);
    const tones = (['SCHEDULED', 'OPEN', 'TALLIED', 'CLOSED', 'DRAFT', 'ARCHIVED'] as const).map((status) => {
      fixture.componentRef.setInput('status', status);
      return (fixture.componentInstance as unknown as { tone(): string }).tone();
    });
    expect(tones).toEqual(['info', 'ok', 'ok', 'warn', 'neutral', 'neutral']);
  });

  it('opens, toggles and closes the side menu', () => {
    const nav = new AdminNavService();
    nav.toggle();
    expect(nav.expanded()).toBe(true);
    nav.toggle();
    expect(nav.expanded()).toBe(false);
    nav.toggle();
    nav.close();
    expect(nav.expanded()).toBe(false);
  });
});
