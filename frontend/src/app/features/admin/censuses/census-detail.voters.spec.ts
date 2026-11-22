import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { adminProviders } from '../../../../testing/admin-test-doubles';
import { ApiError } from '../../../core/admin/api-error';
import { CensusApiService, Voter } from '../../../core/admin/census-api.service';
import { CensusDetailPage } from './census-detail.page';

/** The page's protected members used by the people list, the manual form and the removal (A2). */
type Page = {
  ionViewWillEnter(): void;
  ngOnDestroy(): void;
  census(): { voterCount: number } | null;
  loading(): boolean;
  loadError(): boolean;
  voters(): Voter[];
  total(): number;
  votersError(): boolean;
  page(): number;
  onSearch(value: string | null | undefined): void;
  goToPage(delta: number): void;
  removeButtons(): { role: string }[];
  file: { set(file: File | null): void };
  phase(): string;
  imported(): number | null;
  summary(): unknown;
  importError(): string | null;
  apply(discardInvalidLines: boolean): void;
  form(): Record<string, unknown> | null;
  formErrors(): Record<string, string>;
  formServerError(): string | null;
  notice(): string | null;
  pageError(): string | null;
  openNew(): void;
  openEdit(voter: Voter): void;
  closeForm(): void;
  setField(field: string, value: string | null | undefined): void;
  save(): Promise<void>;
  removing: { set(voter: Voter | null): void };
  onRemoveDismiss(role: string | undefined): Promise<void>;
};

const CENSUS = { id: 7, name: 'Censo', voterCount: 2, frozen: false, createdAt: '2026-11-01T10:00:00Z' };
const voter = (id: number, over: Partial<Voter> = {}): Voter => ({
  id, fullName: `Persona ${id}`, email: `p${id}@example.com`, phone: null, institutionalId: 'SINT-1', preferredLanguage: 'es', ...over,
});

/** A2 (RF-03): people of a census, manual add and edit, and removal while it is not frozen. Synthetic data only. */
describe('CensusDetailPage (A2, personas)', () => {
  let api: Record<'get' | 'voters' | 'addVoter' | 'updateVoter' | 'removeVoter' | 'importVoters', ReturnType<typeof vi.fn>>;
  let page: Page;
  const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

  beforeEach(() => {
    api = {
      get: vi.fn().mockResolvedValue(CENSUS),
      voters: vi.fn().mockResolvedValue({ items: [voter(1), voter(2)], page: 0, size: 25, totalItems: 2 }),
      addVoter: vi.fn().mockImplementation(async (_: number, data: object) => ({ id: 3, ...data })),
      updateVoter: vi.fn().mockImplementation(async (_: number, id: number, data: object) => ({ id, ...data })),
      removeVoter: vi.fn().mockResolvedValue(undefined),
      importVoters: vi.fn().mockReturnValue(of({ applied: true, toCreate: 4, duplicates: [], invalidLines: [] })),
    };
    TestBed.configureTestingModule({ providers: [...adminProviders(['CENSUS_MANAGER']), { provide: CensusApiService, useValue: api }] });
    const fixture = TestBed.createComponent(CensusDetailPage);
    fixture.componentRef.setInput('id', '7');
    page = fixture.componentInstance as unknown as Page;
  });
  afterEach(() => {
    page.ngOnDestroy();
    vi.useRealTimers();
  });

  it('loads the census and its first page of people', async () => {
    page.ionViewWillEnter();
    await settle();
    expect(page.census()).toEqual(CENSUS);
    expect(page.voters()).toHaveLength(2);
    expect(page.total()).toBe(2);
    expect(page.loading()).toBe(false);
    expect(page.removeButtons().map((b) => b.role)).toEqual(['cancel', 'confirm']);
  });

  it('flags a census or a list that cannot be read', async () => {
    api.get.mockRejectedValueOnce(new Error('down'));
    page.ionViewWillEnter();
    await settle();
    expect(page.loadError()).toBe(true);
    api.voters.mockRejectedValueOnce(new Error('down'));
    page.goToPage(1);
    await settle();
    expect(page.votersError()).toBe(true);
  });

  it('searches after a pause, from the first page', async () => {
    vi.useFakeTimers();
    page.goToPage(2);
    page.onSearch('ana');
    page.onSearch(null);
    page.onSearch('maria');
    await vi.advanceTimersByTimeAsync(300);
    expect(page.page()).toBe(0);
    expect(api.voters).toHaveBeenLastCalledWith(7, 0, expect.any(Number), 'maria');
    page.goToPage(-4);
    expect(page.page()).toBe(0);
  });

  it('applies an import and reloads the census and the list', async () => {
    page.apply(false);
    expect(api.importVoters).not.toHaveBeenCalled();
    page.file.set(new File(['x'], 'censo.xlsx'));
    page.apply(true);
    await settle();
    expect(api.importVoters).toHaveBeenCalledWith(7, expect.any(File), false, true);
    expect(page.phase()).toBe('done');
    expect(page.imported()).toBe(4);
  });

  it('shows the new summary when the census changed since the dry run, and the failure of the upload', async () => {
    page.file.set(new File(['x'], 'censo.xlsx'));
    api.importVoters.mockReturnValueOnce(of({ applied: false, toCreate: 1, duplicates: [], invalidLines: [{ line: 3, message: 'x' }] }));
    page.apply(false);
    await settle();
    expect(page.phase()).toBe('summary');
    expect(page.importError()).toBe('admin.census.import.notApplied');
    api.importVoters.mockReturnValueOnce(throwError(() => new Error('down')));
    page.apply(false);
    await settle();
    expect(page.importError()).not.toBeNull();
  });

  it('validates the manual form before sending it', async () => {
    await page.save();
    page.openNew();
    page.setField('fullName', ' ');
    page.setField('email', 'no-es-un-correo');
    page.setField('phone', '+34 600');
    await page.save();
    expect(Object.keys(page.formErrors()).sort()).toEqual(['email', 'fullName', 'phone']);
    page.setField('email', 'ana@example.com');
    expect(page.formErrors()['email']).toBeUndefined();
    page.setField('institutionalId', 'x'.repeat(51));
    page.setField('fullName', 'Ana');
    page.setField('phone', null);
    await page.save();
    expect(Object.keys(page.formErrors())).toEqual(['institutionalId']);
    expect(api.addVoter).not.toHaveBeenCalled();
  });

  it('adds a person and edits another, keeping the stored institutional ID that the form does not show', async () => {
    page.openNew();
    page.setField('fullName', ' Ana ');
    page.setField('email', 'ana@example.com');
    page.setField('phone', '600123456');
    await page.save();
    expect(api.addVoter).toHaveBeenCalledWith(7, { fullName: 'Ana', email: 'ana@example.com', phone: '600123456',
      institutionalId: null, preferredLanguage: 'es' });
    expect(page.form()).toBeNull();
    expect(page.notice()).toBe('admin.census.form.added');

    page.openEdit(voter(2, { phone: '600000000', preferredLanguage: 'en' }));
    page.setField('fullName', 'Persona Dos');
    await page.save();
    expect(api.updateVoter).toHaveBeenCalledWith(7, 2, expect.objectContaining({ fullName: 'Persona Dos', institutionalId: 'SINT-1',
      preferredLanguage: 'en' }));
    expect(page.notice()).toBe('admin.census.form.updated');
    page.openNew();
    page.closeForm();
    expect(page.form()).toBeNull();
  });

  it('shows the field error of the service, its detail or a generic error', async () => {
    page.openNew();
    page.setField('fullName', 'Ana');
    page.setField('email', 'ana@example.com');
    api.addVoter.mockRejectedValueOnce(new ApiError(422, 'Datos no válidos', [{ field: 'email', message: 'Repetido' }]));
    await page.save();
    expect(page.formErrors()).toEqual({ email: 'Repetido' });
    api.addVoter.mockRejectedValueOnce(new ApiError(409, 'Censo congelado', [{ field: 'language', message: 'x' }]));
    await page.save();
    expect(page.formServerError()).toBe('Censo congelado');
    api.addVoter.mockRejectedValueOnce(new ApiError(500, null, []));
    await page.save();
    expect(page.formServerError()).toBe('common.loadError');
    api.addVoter.mockRejectedValueOnce(new Error('offline'));
    await page.save();
    expect(page.formServerError()).toBe('common.loadError');
  });

  it('removes a person only after the confirmation, going back a page when it was the last one', async () => {
    page.ionViewWillEnter();
    await settle();
    page.removing.set(voter(1));
    await page.onRemoveDismiss('cancel');
    expect(api.removeVoter).not.toHaveBeenCalled();

    api.voters.mockResolvedValue({ items: [voter(9)], page: 1, size: 25, totalItems: 26 });
    page.goToPage(1);
    await settle();
    page.removing.set(voter(9));
    await page.onRemoveDismiss('confirm');
    expect(api.removeVoter).toHaveBeenCalledWith(7, 9);
    expect(page.notice()).toBe('admin.census.remove.done');
    expect(page.page()).toBe(0);
  });

  it('shows why a person could not be removed', async () => {
    page.removing.set(voter(1));
    api.removeVoter.mockRejectedValueOnce(new ApiError(409, 'Censo congelado', []));
    await page.onRemoveDismiss('confirm');
    expect(page.pageError()).toBe('Censo congelado');
    page.removing.set(voter(1));
    api.removeVoter.mockRejectedValueOnce(new Error('offline'));
    await page.onRemoveDismiss('confirm');
    expect(page.pageError()).toBe('common.loadError');
  });
});
