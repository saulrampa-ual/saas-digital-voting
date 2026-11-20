import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { adminProviders, stubDownloads } from '../../../../testing/admin-test-doubles';
import { ApiError } from '../../../core/admin/api-error';
import { PrivacyApiService, PrivacySubject } from '../../../core/admin/privacy-api.service';
import { PrivacyPage } from './privacy.page';

type Page = {
  query: { set(v: string): void; (): string };
  confirmed: { set(v: boolean): void };
  subject(): PrivacySubject | null;
  findError(): string | null;
  exportMessage(): string | null;
  exportError(): string | null;
  eraseError(): string | null;
  done(): string | null;
  processCount(): number;
  blockingTitles(): string;
  find(): Promise<void>;
  exportData(): Promise<void>;
  erase(): Promise<void>;
};

const SUBJECT: PrivacySubject = {
  fullName: 'Persona Sintética',
  entries: [
    { census: 'Censo A', fullName: 'Persona Sintética', email: 'p@example.com', phone: null, institutionalId: 'SINT-1',
      preferredLanguage: 'es', addedAt: '2026-11-01T10:00:00Z', erasure: 'DELETE',
      processes: [{ id: 1, title: { es: 'Uno' }, status: 'OPEN' }, { id: 2, title: { es: 'Dos' }, status: 'TALLIED' }] },
    { census: 'Censo B', fullName: 'Persona Sintética', email: 'p@example.com', phone: null, institutionalId: null,
      preferredLanguage: 'es', addedAt: '2026-11-02T10:00:00Z', erasure: 'ANONYMIZE',
      processes: [{ id: 2, title: { es: 'Dos' }, status: 'TALLIED' }] },
  ],
  erasureAllowed: false,
  blockingProcesses: [{ id: 1, title: { es: 'Uno' }, status: 'OPEN' }, { id: 3, title: { en: 'Three' }, status: 'OPEN' }],
  toDelete: 1,
  toAnonymize: 1,
} as PrivacySubject;

/** G2 (RF-18, RNF-16): search, export and erase the data of one person; the vote is never part of it. */
describe('PrivacyPage (protección de datos)', () => {
  let api: Record<'search' | 'export' | 'erase', ReturnType<typeof vi.fn>>;
  let page: Page;
  let fixture: ComponentFixture<unknown>;

  beforeEach(() => {
    stubDownloads();
    api = {
      search: vi.fn().mockResolvedValue(SUBJECT),
      export: vi.fn().mockResolvedValue(new Blob(['{}'])),
      erase: vi.fn().mockResolvedValue({ deleted: 1, anonymized: 1 }),
    };
    TestBed.configureTestingModule({ providers: [...adminProviders(), { provide: PrivacyApiService, useValue: api }] });
    fixture = TestBed.createComponent(PrivacyPage);
    page = fixture.componentInstance as unknown as Page;
  });
  // Render the last state of each test, so the template is checked with the data too
  afterEach(async () => {
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('does not search with an empty query', async () => {
    page.query.set('   ');
    await page.find();
    expect(api.search).not.toHaveBeenCalled();
  });

  it('finds the person, counts their processes and names what blocks the erasure', async () => {
    page.query.set(' p@example.com ');
    await page.find();
    expect(api.search).toHaveBeenCalledWith('p@example.com');
    expect(page.processCount()).toBe(2);
    expect(page.blockingTitles()).toBe('«Uno», «3»');
  });

  it('shows the detail of the service when the search fails, but not that of a 401', async () => {
    page.query.set('x');
    api.search.mockRejectedValueOnce(new ApiError(404, 'No hay nadie con ese dato', []));
    await page.find();
    expect(page.findError()).toBe('No hay nadie con ese dato');
    api.search.mockRejectedValueOnce(new ApiError(401, 'Sesión caducada', []));
    await page.find();
    expect(page.findError()).toBe('common.loadError');
  });

  it('exports the data of the person shown', async () => {
    page.query.set('p@example.com');
    await page.find();
    await page.exportData();
    expect(api.export).toHaveBeenCalledWith('p@example.com');
    expect(page.exportMessage()).toBe('admin.privacy.exported');
    api.export.mockRejectedValueOnce(new Error('down'));
    await page.exportData();
    expect(page.exportError()).toBe('common.loadError');
  });

  it('erases only after the confirmation, and clears the screen', async () => {
    page.query.set('p@example.com');
    await page.find();
    await page.erase();
    expect(api.erase).not.toHaveBeenCalled();
    page.confirmed.set(true);
    await page.erase();
    expect(api.erase).toHaveBeenCalledWith('p@example.com');
    expect(page.subject()).toBeNull();
    expect(page.query()).toBe('');
    expect(page.done()).toBe('admin.privacy.erased');
  });

  it('keeps the person on screen when the erasure is refused', async () => {
    page.query.set('p@example.com');
    await page.find();
    page.confirmed.set(true);
    api.erase.mockRejectedValueOnce(new ApiError(409, 'Tiene procesos abiertos', []));
    await page.erase();
    expect(page.eraseError()).toBe('Tiene procesos abiertos');
    expect(page.subject()).not.toBeNull();
  });
});
