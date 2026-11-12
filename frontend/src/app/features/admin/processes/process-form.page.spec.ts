import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService, clearProcessDrafts } from '../../../core/auth/auth.service';
import { ProcessApiService, ProcessData } from '../../../core/admin/process-api.service';
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
  fallback(text: Record<'es' | 'en', string>, lang: 'es' | 'en'): string;
  pending(lang: 'es' | 'en'): boolean;
  model(): { title: Record<'es' | 'en', string>; opensAt: string; closesAt: string };
  step(): string;
  stepErrors(): { field: string; message: string }[];
  restoredAt(): string | null;
  duration(): { key: string; params: { time: string } } | null;
};

const DRAFT_KEY = 'vv.processDraft:user-1:new';

function localInput(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

describe('ProcessFormPage (A3)', () => {
  let api: { censuses: ReturnType<typeof vi.fn>; create: ReturnType<typeof vi.fn>; get: ReturnType<typeof vi.fn>; update: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    api = {
      censuses: vi.fn().mockResolvedValue([{ id: 7, name: 'Censo', voterCount: 3, frozen: false }]),
      create: vi.fn().mockImplementation(async (data: ProcessData) => ({ id: 99, ...data })),
      get: vi.fn(),
      update: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [
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

  it('saves a text written only in the default language in both languages', async () => {
    const page = await newPage();
    await fillValid(page);
    expect(page.pending('en')).toBe(true);
    expect(page.fallback(page.model().title, 'en')).toBe('Elección de prueba');
    await page.save();
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
