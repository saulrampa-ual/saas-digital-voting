import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { Subject, of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { CSV_MAX_BYTES, CensusApiService, ImportSummary } from '../../../core/admin/census-api.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { CensusDetailPage } from './census-detail.page';

/** The page's protected members, reached from the test (no template rendering: only the import logic of A2). */
type Page = {
  census: { set(value: object | null): void };
  phase(): string;
  file(): File | null;
  summary(): ImportSummary | null;
  importError(): string | null;
  dragging(): boolean;
  templateOpen(): boolean;
  onFileInput(event: Event): void;
  onDrop(event: DragEvent): void;
  onDragOver(event: DragEvent): void;
  cancelValidation(): void;
  toggleTemplate(): void;
  closeTemplate(): void;
  downloadTemplate(): void;
  exampleLines(summary: ImportSummary): string;
  reasonKey(reason: string): string;
};

const SUMMARY: ImportSummary = { applied: false, toCreate: 2, duplicates: [], invalidLines: [] };

/** A2 (RF-03, D-27): client checks of the census import and the downloadable template. Synthetic data only. */
describe('CensusDetailPage (A2, import)', () => {
  let api: { get: ReturnType<typeof vi.fn>; importVoters: ReturnType<typeof vi.fn> };
  let manager: boolean;

  beforeEach(() => {
    manager = true;
    api = {
      get: vi.fn().mockResolvedValue({ id: 7, name: 'Censo', voterCount: 0, frozen: false }),
      importVoters: vi.fn().mockReturnValue(of(SUMMARY)),
    };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideTranslateService(),
        { provide: CensusApiService, useValue: api },
        { provide: AuthService, useValue: { hasAnyRole: () => manager } },
        { provide: LanguageService, useValue: { current: signal('es') } },
      ],
    });
  });
  afterEach(() => vi.restoreAllMocks());

  function create(frozen = false): Page {
    const fixture = TestBed.createComponent(CensusDetailPage);
    fixture.componentRef.setInput('id', '7');
    const page = fixture.componentInstance as unknown as Page;
    page.census.set({ id: 7, name: 'Censo', voterCount: 0, frozen });
    return page;
  }

  const xlsx = (name = 'censo.xlsx', content = 'x') => new File([content], name);
  const chosen = (file: File) => ({ target: { files: [file], value: 'C:\\fakepath\\' + file.name } }) as unknown as Event;
  const dropped = (file: File) => ({ preventDefault: vi.fn(), dataTransfer: { files: [file] } }) as unknown as DragEvent;

  it('validates an Excel file with a dry run and shows the summary', () => {
    const page = create();
    const file = xlsx();
    const event = chosen(file);
    page.onFileInput(event);

    expect((event.target as HTMLInputElement).value).toBe('');
    expect(api.importVoters).toHaveBeenCalledWith(7, file, true);
    expect(page.phase()).toBe('summary');
    expect(page.summary()).toEqual(SUMMARY);
    expect(page.importError()).toBeNull();
  });

  it('rejects other formats, empty files and files over the size limit before uploading', () => {
    const page = create();
    const tooBig = xlsx();
    Object.defineProperty(tooBig, 'size', { value: CSV_MAX_BYTES + 1 });

    for (const file of [xlsx('censo.csv'), xlsx('censo.xlsx', ''), tooBig]) {
      page.onFileInput(chosen(file));
      expect(page.importError()).not.toBeNull();
      expect(page.phase()).toBe('idle');
    }
    expect(api.importVoters).not.toHaveBeenCalled();
  });

  it('does nothing when no file was chosen', () => {
    const page = create();
    page.onFileInput({ target: { files: [], value: '' } } as unknown as Event);
    expect(page.file()).toBeNull();
    expect(api.importVoters).not.toHaveBeenCalled();
  });

  it('lets the dry run be cancelled', () => {
    const pending = new Subject<ImportSummary>();
    api.importVoters.mockReturnValue(pending);
    const page = create();
    page.onFileInput(chosen(xlsx()));
    expect(page.phase()).toBe('validating');

    page.cancelValidation();
    expect(pending.observed).toBe(false);
    expect(page.phase()).toBe('idle');
    expect(page.file()).toBeNull();
  });

  it('explains a file that is too large for the service', () => {
    api.importVoters.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 413 })));
    const page = create();
    page.onFileInput(chosen(xlsx()));

    expect(page.importError()).toBe('admin.census.import.errors.size');
    expect(page.phase()).toBe('idle');
    expect(api.get).not.toHaveBeenCalled();
  });

  it('shows the detail given by the service and reloads a census frozen meanwhile', () => {
    api.importVoters.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'Censo congelado' } })));
    const page = create();
    page.onFileInput(chosen(xlsx()));

    expect(page.importError()).toBe('Censo congelado');
    expect(page.phase()).toBe('idle');
    expect(api.get).toHaveBeenCalledWith(7);
  });

  it('shows a generic message for any other failure', () => {
    api.importVoters.mockReturnValue(throwError(() => new Error('network')));
    const page = create();
    page.onFileInput(chosen(xlsx()));

    expect(page.importError()).toBe('admin.census.import.errors.generic');
    expect(page.phase()).toBe('idle');
  });

  it('accepts a dropped file only from a census manager while the census is not frozen', () => {
    const open = create();
    const over = { preventDefault: vi.fn() } as unknown as DragEvent;
    open.onDragOver(over);
    expect(over.preventDefault).toHaveBeenCalled();
    expect(open.dragging()).toBe(true);
    open.onDrop(dropped(xlsx()));
    expect(open.dragging()).toBe(false);
    expect(api.importVoters).toHaveBeenCalledTimes(1);

    const frozen = create(true);
    frozen.onDragOver(over);
    expect(frozen.dragging()).toBe(false);
    frozen.onDrop(dropped(xlsx()));
    frozen.onDrop({ preventDefault: vi.fn() } as unknown as DragEvent);
    expect(api.importVoters).toHaveBeenCalledTimes(1);
  });

  it('opens and closes the template panel, and choosing a file closes it', () => {
    const page = create();
    page.toggleTemplate();
    expect(page.templateOpen()).toBe(true);
    page.toggleTemplate();
    expect(page.templateOpen()).toBe(false);

    page.toggleTemplate();
    page.onFileInput(chosen(xlsx()));
    expect(page.templateOpen()).toBe(false);
  });

  it('downloads the template of the interface language', () => {
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    create().downloadTemplate();
    expect(click).toHaveBeenCalledTimes(1);
  });

  it('lists the first example lines of the summary and builds the reason keys', () => {
    const page = create();
    const lines = Array.from({ length: 25 }, (_, i) => i + 2);

    expect(page.exampleLines({ ...SUMMARY, exampleLines: lines })).toBe(lines.slice(0, 20).join(', '));
    expect(page.exampleLines(SUMMARY)).toBe('');
    expect(page.reasonKey('EMAIL')).toBe('admin.census.import.reason.EMAIL');
  });
});
