import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService, clearProcessDrafts } from '../../../core/auth/auth.service';
import { ProcessApiService, ProcessData } from '../../../core/admin/process-api.service';
import { TextTranslationService } from '../../../core/admin/text-translation.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { ProcessFormPage } from './process-form.page';

/** The page's protected members, reached from the test (no template rendering: only the logic of A3). */
type Page = {
  ngOnInit(): Promise<void>;
  update(change: object): void;
  setText(field: 'title' | 'description' | 'question', lang: 'es' | 'en', value: string): void;
  setOption(index: number, lang: 'es' | 'en', value: string): void;
  next(): void;
  save(): Promise<void>;
  discardDraft(): void;
  continueDraft(): void;
  reviewOpen(): boolean;
  shownLangs(): readonly ('es' | 'en')[];
  fallback(text: Record<'es' | 'en', string>, lang: 'es' | 'en'): string;
  model(): {
    title: Record<'es' | 'en', string>; opensAt: string; closesAt: string; censusId: number | null;
    options: Record<'es' | 'en', string>[]; auto: Record<string, string>;
  };
  step(): string;
  stepErrors(): { field: string; message: string }[];
  restoredAt(): string | null;
  duration(): { key: string; params: { time: string } } | null;
  onlyCensus(): boolean;
  translateMissing(): Promise<boolean>;
  translationFailed(): boolean;
  isAuto(key: string, lang: 'es' | 'en'): boolean;
  chip(lang: 'es' | 'en'): 'pending' | 'auto' | null;
  removeOption(index: number): void;
};

const DRAFT_KEY = 'vv.processDraft:user-1:new';

function localInput(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

describe('ProcessFormPage (A3)', () => {
  /** Fake LibreTranslate: «EN:» + text, or unavailable (null). */
  let translator: { translate: ReturnType<typeof vi.fn> };
  let api: { censuses: ReturnType<typeof vi.fn>; create: ReturnType<typeof vi.fn>; get: ReturnType<typeof vi.fn>; update: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    api = {
      censuses: vi.fn().mockResolvedValue([{ id: 7, name: 'Censo', voterCount: 3, frozen: false }]),
      create: vi.fn().mockImplementation(async (data: ProcessData) => ({ id: 99, ...data })),
      get: vi.fn(),
      update: vi.fn(),
    };
    translator = { translate: vi.fn().mockImplementation(async (texts: string[]) => texts.map((t) => `EN:${t}`)) };
    TestBed.configureTestingModule({
      providers: [
        { provide: TextTranslationService, useValue: translator },
        provideRouter([]),
        provideTranslateService(),
        { provide: ProcessApiService, useValue: api },
        { provide: AuthService, useValue: { userId: signal('user-1') } },
        { provide: LanguageService, useValue: { current: signal('es') } },
      ],
    });
  });

  afterEach(() => localStorage.clear());

  async function newPage(): Promise<Page> {
    const page = TestBed.runInInjectionContext(() => new ProcessFormPage()) as unknown as Page;
    await page.ngOnInit();
    TestBed.tick();
    return page;
  }

  async function fillValid(page: Page): Promise<void> {
    const opens = new Date(Date.now() + 60 * 60 * 1000);
    const closes = new Date(opens.getTime() + 6 * 60 * 60 * 1000);
    page.setText('title', 'es', 'Elección de prueba');
    page.next();
    page.update({ opensAt: localInput(opens), closesAt: localInput(closes) });
    page.next();
    page.update({ censusId: 7 });
    page.next();
    page.setText('question', 'es', '¿A quién votas?');
    page.setOption(0, 'es', 'Ana');
    page.setOption(1, 'es', 'Luis');
  }

  it('translates the texts written only in the default language before saving', async () => {
    const page = await newPage();
    await fillValid(page);
    expect(page.chip('en')).toBe('pending');
    expect(page.fallback(page.model().title, 'en')).toBe('admin.processForm.willTranslate');
    await page.save();
    const data = api.create.mock.calls[0][0] as ProcessData;
    expect(translator.translate).toHaveBeenCalledWith(['Elección de prueba', '¿A quién votas?', 'Ana', 'Luis'], 'es', 'en');
    expect(data.title).toEqual({ es: 'Elección de prueba', en: 'EN:Elección de prueba' });
    expect(data.description).toEqual({});
    expect(data.ballot.question).toEqual({ es: '¿A quién votas?', en: 'EN:¿A quién votas?' });
    expect(data.ballot.options.map((o) => o.text)).toEqual([{ es: 'Ana', en: 'EN:Ana' }, { es: 'Luis', en: 'EN:Luis' }]);
  });

  it('marks a machine text until the person edits it, and keeps the edit', async () => {
    const page = await newPage();
    page.setText('title', 'es', 'Elección');
    await page.translateMissing();
    expect(page.model().title.en).toBe('EN:Elección');
    expect(page.isAuto('title', 'en')).toBe(true);
    expect(page.chip('en')).toBe('auto');
    // The source changes: the automatic text follows it
    page.setText('title', 'es', 'Elección 2026');
    await page.translateMissing();
    expect(page.model().title.en).toBe('EN:Elección 2026');
    // The person corrects the English text: it is theirs and is never overwritten again
    page.setText('title', 'en', 'Election 2026');
    expect(page.isAuto('title', 'en')).toBe(false);
    page.setText('title', 'es', 'Elección de 2026');
    await page.translateMissing();
    expect(page.model().title.en).toBe('Election 2026');
  });

  it('keeps the automatic marks of the following options when one is removed', async () => {
    const page = await newPage();
    page.setOption(0, 'es', 'Ana');
    page.setOption(1, 'es', 'Luis');
    (page as unknown as { addOption(): void }).addOption();
    page.setOption(2, 'es', 'Eva');
    await page.translateMissing();
    page.setOption(1, 'en', 'Louis');
    page.removeOption(0);
    expect(page.model().options.map((o) => o.en)).toEqual(['Louis', 'EN:Eva']);
    expect(page.model().auto).toEqual({ 'options.1': 'Eva' });
  });

  it('saves the same text in both languages when the translator is not available', async () => {
    translator.translate.mockResolvedValue(null);
    const page = await newPage();
    await fillValid(page);
    await page.save();
    expect(page.translationFailed()).toBe(true);
    const data = api.create.mock.calls[0][0] as ProcessData;
    expect(data.title).toEqual({ es: 'Elección de prueba', en: 'Elección de prueba' });
    expect(data.description).toEqual({});
    expect(data.ballot.question).toEqual({ es: '¿A quién votas?', en: '¿A quién votas?' });
    expect(data.ballot.options.map((o) => o.text)).toEqual([{ es: 'Ana', en: 'Ana' }, { es: 'Luis', en: 'Luis' }]);
  });

  it('keeps an English text that was written', async () => {
    const page = await newPage();
    await fillValid(page);
    page.setText('title', 'en', 'Test election');
    await page.save();
    expect((api.create.mock.calls[0][0] as ProcessData).title).toEqual({ es: 'Elección de prueba', en: 'Test election' });
    expect(translator.translate.mock.calls[0][0]).not.toContain('Elección de prueba');
  });

  it('forgets the automatic marks when the default language changes', async () => {
    const page = await newPage();
    page.setText('title', 'es', 'Elección');
    await page.translateMissing();
    page.update({ defaultLanguage: 'en' });
    expect(page.model().auto).toEqual({});
  });

  it('rejects an opening in the past and a closing equal to the opening', async () => {
    const page = await newPage();
    page.setText('title', 'es', 'X');
    page.next();
    const past = localInput(new Date(Date.now() - 5 * 60 * 1000));
    page.update({ opensAt: past, closesAt: past });
    page.next();
    expect(page.step()).toBe('calendar');
    expect(page.stepErrors().map((e) => e.message)).toEqual([
      'admin.processForm.errors.opensFuture', 'admin.processForm.errors.closesAfter',
    ]);
  });

  it('shows how long the voting will be open', async () => {
    const page = await newPage();
    page.update({ opensAt: '2026-12-01T10:00', closesAt: '2026-12-02T12:30' });
    expect(page.duration()?.params.time).toBe('1 d 2 h 30 min');
  });

  it('restores unsaved work after a reload and forgets it on save', async () => {
    const first = await newPage();
    first.setText('title', 'es', 'Borrador sin guardar');
    first.next();
    TestBed.tick();
    expect(JSON.parse(localStorage.getItem(DRAFT_KEY)!).model.title.es).toBe('Borrador sin guardar');

    const reloaded = await newPage();
    expect(reloaded.model().title.es).toBe('Borrador sin guardar');
    expect(reloaded.step()).toBe('calendar');
    expect(reloaded.restoredAt()).not.toBeNull();

    await fillValid(reloaded);
    await reloaded.save();
    TestBed.tick();
    expect(localStorage.getItem(DRAFT_KEY)).toBeNull();
  });

  it('does not keep anything until the person changes something', async () => {
    await newPage();
    expect(localStorage.getItem(DRAFT_KEY)).toBeNull();
  });

  it('«Seguir donde lo dejé» closes the notice and keeps what was written', async () => {
    const first = await newPage();
    first.setText('title', 'es', 'Algo');
    first.next();
    TestBed.tick();
    const reloaded = await newPage();
    reloaded.continueDraft();
    TestBed.tick();
    expect(reloaded.restoredAt()).toBeNull();
    expect(reloaded.model().title.es).toBe('Algo');
    expect(reloaded.step()).toBe('calendar');
    expect(localStorage.getItem(DRAFT_KEY)).not.toBeNull();
  });

  it('shows only the language of the texts until the translation is reviewed or has an error', async () => {
    const page = await newPage();
    expect(page.shownLangs()).toEqual(['es']);
    page.update({ defaultLanguage: 'en' });
    expect(page.shownLangs()).toEqual(['en']);
    page.setText('title', 'en', 'Test election');
    page.setText('title', 'es', 'x'.repeat(151));
    page.next();
    expect(page.step()).toBe('basics');
    expect(page.reviewOpen()).toBe(true);
    expect(page.shownLangs()).toEqual(['en', 'es']);
  });

  it('«Empezar de cero» goes back to the empty form', async () => {
    const first = await newPage();
    first.setText('title', 'es', 'Algo');
    TestBed.tick();
    const reloaded = await newPage();
    reloaded.discardDraft();
    TestBed.tick();
    expect(reloaded.model().title.es).toBe('');
    expect(reloaded.step()).toBe('basics');
    expect(localStorage.getItem(DRAFT_KEY)).toBeNull();
  });

  it('discards a draft older than 24 hours', async () => {
    localStorage.setItem(DRAFT_KEY, JSON.stringify({
      savedAt: new Date(Date.now() - 25 * 60 * 60 * 1000).toISOString(), base: null, step: 'basics', visited: ['basics'],
      model: { title: { es: 'Viejo', en: '' } },
    }));
    const page = await newPage();
    expect(page.model().title.es).toBe('');
    expect(localStorage.getItem(DRAFT_KEY)).toBeNull();
  });

  it('selects the census when the organization has only one', async () => {
    const page = await newPage();
    expect(page.model().censusId).toBe(7);
    expect(page.onlyCensus()).toBe(true);
    // Not «unsaved work» on its own: nothing is kept until the person changes something
    expect(localStorage.getItem(DRAFT_KEY)).toBeNull();
  });

  it('lets the person choose when there are two or more censuses', async () => {
    api.censuses.mockResolvedValue([
      { id: 7, name: 'A', voterCount: 3, frozen: false }, { id: 8, name: 'B', voterCount: 2, frozen: true },
    ]);
    const page = await newPage();
    expect(page.model().censusId).toBeNull();
    expect(page.onlyCensus()).toBe(false);
  });

  it('clears every draft on logout', () => {
    localStorage.setItem(DRAFT_KEY, '{}');
    localStorage.setItem('vv.processDraft:user-2:5', '{}');
    localStorage.setItem('verivoto.language', 'en');
    clearProcessDrafts();
    expect(localStorage.getItem(DRAFT_KEY)).toBeNull();
    expect(localStorage.getItem('vv.processDraft:user-2:5')).toBeNull();
    expect(localStorage.getItem('verivoto.language')).toBe('en');
  });
});
