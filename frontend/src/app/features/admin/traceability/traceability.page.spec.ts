import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { adminProviders } from '../../../../testing/admin-test-doubles';
import { AdminAction, AdminActionsApiService } from '../../../core/admin/admin-actions-api.service';
import { ProcessApiService } from '../../../core/admin/process-api.service';
import { TraceabilityPage } from './traceability.page';

type Page = {
  ionViewWillEnter(): Promise<void>;
  apply(): Promise<void>;
  clear(): Promise<void>;
  go(delta: number): Promise<void>;
  object(a: AdminAction): string;
  detail(a: AdminAction): string | null;
  processName(id: number | null): string;
  from: { set(v: string): void };
  to: { set(v: string): void };
  actorId: { set(v: string): void };
  action: { set(v: string): void };
  processId: { set(v: string): void };
  actors(): unknown[];
  processes(): unknown[];
  pageIndex(): number;
  first(): number;
  last(): number;
  error(): boolean;
  dateError(): boolean;
};

const action = (over: Partial<AdminAction> = {}): AdminAction => ({
  occurredAt: '2026-11-20T09:00:00Z', actorId: 'u1', actorName: 'Ana', action: 'CENSUS_CREATED', objectType: 'CENSUS',
  objectId: '7', objectLabel: null, detail: null, processId: null, source: 'CONSOLE', ...over,
});

/** H2 (RF-19): traceability of management actions, with filters and pages; voters only by number. */
describe('TraceabilityPage (trazabilidad)', () => {
  let api: { search: ReturnType<typeof vi.fn>; actors: ReturnType<typeof vi.fn> };
  let processApi: { list: ReturnType<typeof vi.fn> };
  let page: Page;
  let fixture: ComponentFixture<unknown>;

  beforeEach(() => {
    api = {
      search: vi.fn().mockResolvedValue({ items: [action()], page: 0, size: 50, totalItems: 30 }),
      actors: vi.fn().mockResolvedValue([{ actorId: 'u1', actorName: 'Ana' }]),
    };
    processApi = { list: vi.fn().mockResolvedValue([{ id: 4, title: { es: 'Elección', en: 'Election' }, status: 'OPEN' },
      { id: 5, title: { en: 'Only English' }, status: 'OPEN' }]) };
    TestBed.configureTestingModule({
      providers: [...adminProviders(['AUDITOR']), { provide: AdminActionsApiService, useValue: api }, { provide: ProcessApiService, useValue: processApi }],
    });
    fixture = TestBed.createComponent(TraceabilityPage);
    page = fixture.componentInstance as unknown as Page;
  });
  // Render the last state of each test, so the template is checked with the data too
  afterEach(async () => {
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('loads the actors, the processes and the first page', async () => {
    await page.ionViewWillEnter();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(api.search).toHaveBeenCalledWith({}, 0, 50);
    expect(page.actors()).toHaveLength(1);
    expect(page.processes()).toHaveLength(2);
    expect(page.first()).toBe(1);
    expect(page.last()).toBe(30);
  });

  it('keeps empty lists when the actors or processes cannot be read, and flags a failed search', async () => {
    api.actors.mockRejectedValue(new Error('x'));
    processApi.list.mockRejectedValue(new Error('x'));
    api.search.mockRejectedValue(new Error('x'));
    await page.ionViewWillEnter();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(page.actors()).toEqual([]);
    expect(page.processes()).toEqual([]);
    expect(page.error()).toBe(true);
    expect(page.first()).toBe(0);
  });

  it('applies the filters (the «to» day included) and rejects an inverted range', async () => {
    page.from.set('2026-11-20');
    page.to.set('2026-11-10');
    await page.apply();
    expect(page.dateError()).toBe(true);
    expect(api.search).not.toHaveBeenCalled();

    page.to.set('2026-11-21');
    page.actorId.set('u1');
    page.action.set('CENSUS_CREATED');
    page.processId.set('4');
    await page.apply();
    expect(page.dateError()).toBe(false);
    const filter = api.search.mock.calls[0][0];
    expect(filter).toEqual(expect.objectContaining({ actorId: 'u1', action: 'CENSUS_CREATED', processId: 4 }));
    expect(new Date(filter.to).getTime()).toBeGreaterThan(new Date(filter.from).getTime());

    await page.clear();
    expect(api.search).toHaveBeenLastCalledWith({}, 0, 50);
  });

  it('moves between pages without going below the first', async () => {
    await page.go(1);
    expect(page.pageIndex()).toBe(1);
    await page.go(-5);
    expect(page.pageIndex()).toBe(0);
  });

  it('describes the object of each action', async () => {
    await page.ionViewWillEnter();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(page.object(action({ objectType: 'VOTER', objectId: '12', objectLabel: 'Censo' }))).toBe('admin.trace.objects.VOTER n.º 12 · Censo');
    expect(page.object(action({ objectType: 'VOTER', objectId: null }))).toBe('admin.trace.objects.VOTER n.º ?');
    expect(page.object(action({ objectLabel: 'Censo 2026' }))).toBe('admin.trace.objects.CENSUS «Censo 2026»');
    expect(page.object(action({ objectType: 'PROCESS', objectId: '4' }))).toBe('admin.trace.objects.PROCESS «Elección»');
    expect(page.object(action({ objectId: '7' }))).toBe('admin.trace.objects.CENSUS 7');
    expect(page.object(action({ objectId: null }))).toBe('admin.trace.objects.CENSUS');
  });

  it('translates the details it knows and keeps the others as they are', () => {
    expect(page.detail(action())).toBeNull();
    expect(page.detail(action({ action: 'PERSONAL_DATA_ERASED', detail: '1:2' }))).toBe('admin.trace.details.erased');
    expect(page.detail(action({ action: 'REMINDER_SCHEDULED', detail: '2026-11-20T10:00:00Z' }))).toBe('admin.trace.details.reminder');
    expect(page.detail(action({ action: 'USER_ROLE_CHANGED', detail: 'AUDITOR' }))).toBe('AUDITOR');
  });

  it('names a process by its title in the language, another language or its number', async () => {
    expect(page.processName(null)).toBe('—');
    expect(page.processName(99)).toBe('n.º 99');
    await page.ionViewWillEnter();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(page.processName(4)).toBe('Elección');
    expect(page.processName(5)).toBe('Only English');
  });
});
