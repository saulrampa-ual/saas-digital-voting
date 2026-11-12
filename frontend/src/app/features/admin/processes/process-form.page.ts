import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, effect, inject, input, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  IonButton, IonCheckbox, IonIcon, IonInput, IonItem, IonLabel, IonRadio, IonRadioGroup, IonSegment,
  IonSegmentButton, IonSpinner, IonText, IonTextarea,
} from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  ApiError, CensusSummary, FieldError, Lang, LocalizedText, Process, ProcessApiService, ProcessData, ResultsVisibility, VoterAuthentication,
  VoteType,
} from '../../../core/admin/process-api.service';
import { AuthService, PROCESS_DRAFT_PREFIX } from '../../../core/auth/auth.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { AdminPageComponent } from '../admin-page.component';

/** Editable copy of `ProcessData` with both languages always present as strings. */
interface FormModel {
  title: Record<Lang, string>;
  description: Record<Lang, string>;
  defaultLanguage: Lang;
  resultsVisibility: ResultsVisibility;
  voterAuthentication: VoterAuthentication;
  opensAt: string;
  closesAt: string;
  censusId: number | null;
  voteType: VoteType;
  question: Record<Lang, string>;
  options: Record<Lang, string>[];
  blankVoteEnabled: boolean;
}

/** Unsaved work kept in this browser so a reload, a closed tab or a detour does not lose it (D-59). */
interface StoredDraft {
  savedAt: string;
  /** Edit only: the process as the server had it; a draft of an older version is not restored. */
  base: string | null;
  model: FormModel;
  step: Step;
  visited: Step[];
}

/** A draft older than this is discarded instead of restored. */
const DRAFT_MAX_AGE_MS = 24 * 60 * 60 * 1000;

const STEPS = ['basics', 'calendar', 'census', 'ballot'] as const;
type Step = (typeof STEPS)[number];
const STEP_OF_FIELD: Record<string, Step> = {
  title: 'basics', description: 'basics', defaultLanguage: 'basics', resultsVisibility: 'basics',
  opensAt: 'calendar', closesAt: 'calendar', censusId: 'census',
};

/**
 * A3 (RF-06, RF-20): create or edit a DRAFT process in four steps. Every text is edited in Spanish and English
 * side by side; only the default language is required. When the other language is left empty, the text of the default
 * language is saved for it too (RI-5), so the process can be scheduled without typing it twice (D-29, D-30). Ends in F3.
 */
@Component({
  selector: 'app-process-form',
  imports: [
    AdminPageComponent, RouterLink, IonButton, IonCheckbox, IonIcon, IonInput, IonItem, IonLabel, IonRadio,
    IonRadioGroup, IonSegment, IonSegmentButton, IonSpinner, IonText, IonTextarea, NgTemplateOutlet, TranslatePipe, LocalDatePipe,
  ],
  templateUrl: './process-form.page.html',
  host: { '(window:beforeunload)': 'beforeUnload($event)' },
  styleUrl: './process-form.page.scss',
})
export class ProcessFormPage implements OnInit {
  /** Present when editing (`/admin/procesos/:id/editar`). */
  readonly id = input<string>();

  private readonly api = inject(ProcessApiService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  private readonly auth = inject(AuthService);
  protected readonly language = inject(LanguageService).current;
  /** Edit only: the process as loaded from the server (fingerprint of `fromProcess`). */
  private base: string | null = null;
  /** True once the person changed something that is not saved yet. */
  private dirty = false;

  protected readonly steps = STEPS;
  protected readonly langs: readonly Lang[] = ['es', 'en'];
  protected readonly step = signal<Step>('basics');
  protected readonly visited = signal<Set<Step>>(new Set(['basics']));
  protected readonly model = signal<FormModel>(emptyModel());
  protected readonly censuses = signal<CensusSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly notEditable = signal(false);
  protected readonly stepErrors = signal<FieldError[]>([]);
  protected readonly serverErrors = signal<FieldError[]>([]);
  protected readonly serverDetail = signal<string | null>(null);
  protected readonly created = signal<{ id: number; title: string } | null>(null);
  /** SR6/SR7: language of the texts shown on phones (below 700 px only one language is visible at a time). */
  protected readonly textTab = signal<Lang>('es');
  /** Time of the unsaved work restored on entering, shown in a notice with «Empezar de cero». */
  protected readonly restoredAt = signal<string | null>(null);

  constructor() {
    // Every change is kept in this browser until the process is saved (D-59).
    effect(() => {
      const model = this.model();
      const step = this.step();
      const visited = this.visited();
      if (this.loading() || this.created() || this.notEditable() || !this.dirty) return;
      this.writeDraft({ savedAt: new Date().toISOString(), base: this.base, model, step, visited: [...visited] });
    });
  }

  /** The browser asks before leaving with unsaved changes (the draft is kept anyway). */
  protected beforeUnload(event: BeforeUnloadEvent): void {
    if (this.dirty && !this.created()) event.preventDefault();
  }

  protected readonly editing = computed(() => !!this.id());
  protected readonly stepIndex = computed(() => STEPS.indexOf(this.step()));
  protected readonly nextStep = computed<Step | null>(() => STEPS[this.stepIndex() + 1] ?? null);
  protected readonly selectedCensus = computed(() => this.censuses().find((c) => c.id === this.model().censusId));

  async ngOnInit(): Promise<void> {
    try {
      this.censuses.set(await this.api.censuses());
      const id = this.id();
      if (id) {
        const process = await this.api.get(Number(id));
        if (process.status !== 'DRAFT') {
          this.notEditable.set(true);
        }
        this.model.set(fromProcess(process));
        this.textTab.set(process.defaultLanguage);
        this.visited.set(new Set(STEPS));
        this.base = JSON.stringify(fromProcess(process));
      }
      if (!this.notEditable()) this.restoreDraft();
    } catch {
      this.serverDetail.set(this.translate.instant('common.loadError'));
    } finally {
      this.loading.set(false);
    }
  }

  protected update(change: Partial<FormModel>): void {
    this.dirty = true;
    this.model.update((model) => ({ ...model, ...change }));
    if (change.defaultLanguage) {
      this.textTab.set(change.defaultLanguage);
    }
    this.stepErrors.set([]);
  }

  /** «mismo texto» chip of the language tabs: the other language has empty texts that will reuse the default language. */
  protected pending(lang: Lang): boolean {
    const m = this.model();
    if (lang === m.defaultLanguage) return false;
    if (this.step() === 'basics') return !m.title[lang].trim();
    if (this.step() === 'ballot') {
      return !m.question[lang].trim() || (m.voteType === 'SINGLE_CHOICE' && m.options.some((option) => !option[lang].trim()));
    }
    return false;
  }

  /** Arrow keys move between the two language tabs (ARIA tabs pattern). */
  protected onTabKey(event: KeyboardEvent): void {
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    event.preventDefault();
    const index = this.langs.indexOf(this.textTab());
    const target = event.key === 'Home' ? 0 : event.key === 'End' ? this.langs.length - 1
      : (index + (event.key === 'ArrowRight' ? 1 : this.langs.length - 1)) % this.langs.length;
    this.textTab.set(this.langs[target]);
    document.getElementById('text-tab-' + this.langs[target])?.focus();
  }

  /**
   * On phones a focused field that ends up behind the on-screen keyboard is scrolled to the middle of the visible area.
   * The Atrás/Siguiente bar stays at the end of the panel (it no longer sticks to the screen and covers other fields).
   */
  protected keepVisible(event: FocusEvent): void {
    if (!window.matchMedia('(max-width: 767.98px)').matches) return;
    const target = event.target as HTMLElement;
    setTimeout(() => {
      const visibleBottom = window.visualViewport?.height ?? window.innerHeight;
      const rect = target.getBoundingClientRect();
      if (rect.bottom > visibleBottom - 8 || rect.top < 0) {
        target.scrollIntoView({ block: 'center' });
      }
    }, 300);
  }

  /** After a validation error, show the language tab that holds the first wrong field if the current one has none. */
  private showErrorLanguage(errors: FieldError[]): void {
    const langs = errors.map((error) => error.field.split('.').pop());
    const first = langs.find((part): part is Lang => part === 'es' || part === 'en');
    if (first && !langs.includes(this.textTab())) {
      this.textTab.set(first);
    }
  }

  /** Placeholder of a text in the other language: the default-language text that will be saved if it stays empty. */
  protected fallback(text: Record<Lang, string>, lang: Lang): string {
    const def = this.model().defaultLanguage;
    return lang === def ? '' : text[def].trim();
  }

  /** Earliest value accepted by the date pickers: the current minute (the opening must be in the future, D-30). */
  protected minDateTime(): string {
    return toLocalInput(new Date().toISOString());
  }

  /** «La votación estará abierta …» under the dates, once both are valid. */
  protected readonly duration = computed<{ key: string; params: object } | null>(() => {
    const { opensAt, closesAt } = this.model();
    if (!opensAt || !closesAt) return null;
    const minutes = Math.round((new Date(closesAt).getTime() - new Date(opensAt).getTime()) / 60000);
    if (!(minutes > 0)) return null;
    const days = Math.floor(minutes / 1440);
    const hours = Math.floor((minutes % 1440) / 60);
    const mins = minutes % 60;
    const time = [days && `${days} d`, hours && `${hours} h`, mins && `${mins} min`].filter(Boolean).join(' ');
    return { key: 'admin.processForm.duration', params: { time } };
  });

  protected setText(field: 'title' | 'description' | 'question', lang: Lang, value: string): void {
    this.dirty = true;
    this.model.update((model) => ({ ...model, [field]: { ...model[field], [lang]: value } }));
  }

  protected setOption(index: number, lang: Lang, value: string): void {
    this.dirty = true;
    this.model.update((model) => ({
      ...model,
      options: model.options.map((option, i) => (i === index ? { ...option, [lang]: value } : option)),
    }));
  }

  protected addOption(): void {
    this.dirty = true;
    this.model.update((model) => ({ ...model, options: [...model.options, { es: '', en: '' }] }));
    setTimeout(() => document.querySelectorAll<HTMLElement>('.option-row ion-input')
      [this.model().options.length * 2 - 2]?.querySelector('input')?.focus(), 50);
  }

  protected removeOption(index: number): void {
    this.dirty = true;
    this.model.update((model) => ({ ...model, options: model.options.filter((_, i) => i !== index) }));
  }

  protected goTo(step: Step): void {
    if (STEPS.indexOf(step) <= this.stepIndex() || this.visited().has(step)) {
      this.step.set(step);
      this.stepErrors.set([]);
      this.focusHeading();
    }
  }

  protected next(): void {
    const errors = this.validate(this.step());
    this.stepErrors.set(errors);
    if (errors.length > 0) {
      this.showErrorLanguage(errors);
      return;
    }
    const nextStep = STEPS[this.stepIndex() + 1];
    this.visited.update((set) => new Set(set).add(nextStep));
    this.step.set(nextStep);
    this.focusHeading();
  }

  protected back(): void {
    this.step.set(STEPS[Math.max(0, this.stepIndex() - 1)]);
    this.stepErrors.set([]);
    this.focusHeading();
  }

  protected async save(): Promise<void> {
    for (const step of STEPS) {
      const errors = this.validate(step);
      if (errors.length > 0) {
        this.step.set(step);
        this.stepErrors.set(errors);
        this.showErrorLanguage(errors);
        return;
      }
    }
    this.saving.set(true);
    this.serverErrors.set([]);
    this.serverDetail.set(null);
    try {
      const data = toProcessData(this.model());
      const id = this.id();
      const saved = id ? await this.api.update(Number(id), data) : await this.api.create(data);
      this.dirty = false;
      this.removeDraft();
      if (id) {
        await this.router.navigate(['/admin/procesos', saved.id], { replaceUrl: true });
      } else {
        this.created.set({ id: saved.id, title: saved.title[saved.defaultLanguage] ?? '' });
        this.focusHeading();
      }
    } catch (error) {
      if (error instanceof ApiError) {
        this.serverDetail.set(error.detail ?? this.translate.instant('common.loadError'));
        this.serverErrors.set(error.errors);
        const first = error.errors[0];
        if (first) {
          this.step.set(first.field.startsWith('ballot') ? 'ballot' : (STEP_OF_FIELD[first.field] ?? this.step()));
          this.showErrorLanguage(error.errors);
        }
      } else {
        this.serverDetail.set(this.translate.instant('common.loadError'));
      }
    } finally {
      this.saving.set(false);
    }
  }

  protected errorFor(field: string): string | null {
    return this.stepErrors().find((error) => error.field === field)?.message ?? null;
  }

  /** Client-side checks with the same limits as the service (RI-4, RI-5); the service has the last word. */
  private validate(step: Step): FieldError[] {
    const m = this.model();
    const t = (key: string, params?: object) => this.translate.instant(`admin.processForm.errors.${key}`, params);
    const errors: FieldError[] = [];
    const def = m.defaultLanguage;
    if (step === 'basics') {
      if (!m.title[def].trim()) errors.push({ field: `title.${def}`, message: t('titleRequired') });
      for (const l of this.langs) {
        if (m.title[l].length > 150) errors.push({ field: `title.${l}`, message: t('max', { max: 150 }) });
        if (m.description[l].length > 1000) errors.push({ field: `description.${l}`, message: t('max', { max: 1000 }) });
        if (m.description[l].trim() && !m.title[l].trim()) {
          errors.push({ field: `title.${l}`, message: t('titleForDescription') });
        }
      }
    }
    if (step === 'calendar') {
      if (!m.opensAt) errors.push({ field: 'opensAt', message: t('opensRequired') });
      else if (new Date(m.opensAt) <= new Date()) errors.push({ field: 'opensAt', message: t('opensFuture') });
      if (!m.closesAt) errors.push({ field: 'closesAt', message: t('closesRequired') });
      if (m.opensAt && m.closesAt && new Date(m.closesAt) <= new Date(m.opensAt)) {
        errors.push({ field: 'closesAt', message: t('closesAfter') });
      }
    }
    if (step === 'census' && m.censusId === null) {
      errors.push({ field: 'censusId', message: t('censusRequired') });
    }
    if (step === 'ballot') {
      if (!m.question[def].trim()) errors.push({ field: `question.${def}`, message: t('questionRequired') });
      for (const l of this.langs) {
        if (m.question[l].length > 300) errors.push({ field: `question.${l}`, message: t('max', { max: 300 }) });
      }
      if (m.voteType === 'SINGLE_CHOICE') {
        if (m.options.length < 2) errors.push({ field: 'options', message: t('minOptions') });
        if (m.options.length > 50) errors.push({ field: 'options', message: t('maxOptions') });
        m.options.forEach((option, i) => {
          if (!option[def].trim()) errors.push({ field: `options.${i}.${def}`, message: t('optionRequired') });
          for (const l of this.langs) {
            if (option[l].length > 200) errors.push({ field: `options.${i}.${l}`, message: t('max', { max: 200 }) });
          }
        });
      }
    }
    return errors;
  }

  /** «Empezar de cero»: forgets the restored work and goes back to the empty form (or to the saved process). */
  protected discardDraft(): void {
    this.removeDraft();
    this.dirty = false;
    this.restoredAt.set(null);
    this.model.set(this.base ? (JSON.parse(this.base) as FormModel) : emptyModel());
    this.textTab.set(this.model().defaultLanguage);
    this.step.set('basics');
    this.visited.set(this.base ? new Set(STEPS) : new Set(['basics']));
    this.stepErrors.set([]);
    this.focusHeading();
  }

  /** Per person (Keycloak `sub`) and per process, so two people sharing a browser never see each other's work. */
  private draftKey(): string | null {
    const user = this.auth.userId();
    return user ? `${PROCESS_DRAFT_PREFIX}${user}:${this.id() ?? 'new'}` : null;
  }

  private restoreDraft(): void {
    const key = this.draftKey();
    if (!key) return;
    try {
      const raw = localStorage.getItem(key);
      if (!raw) return;
      const draft = JSON.parse(raw) as StoredDraft;
      const age = Date.now() - new Date(draft.savedAt).getTime();
      if (!(age >= 0 && age < DRAFT_MAX_AGE_MS) || draft.base !== this.base || !draft.model || !STEPS.includes(draft.step)) {
        localStorage.removeItem(key);
        return;
      }
      // Empty model first, so a field added after the draft was written still has a value.
      this.model.set({ ...emptyModel(), ...draft.model });
      this.step.set(draft.step);
      this.visited.set(new Set([...(draft.visited ?? []), draft.step]));
      this.textTab.set(this.model().defaultLanguage);
      this.restoredAt.set(draft.savedAt);
      this.dirty = true;
    } catch {
      // Unreadable or blocked storage: start as usual.
    }
  }

  private writeDraft(draft: StoredDraft): void {
    const key = this.draftKey();
    if (!key) return;
    try {
      localStorage.setItem(key, JSON.stringify(draft));
    } catch {
      // Storage full or blocked: the form keeps working, only the recovery is lost.
    }
  }

  private removeDraft(): void {
    const key = this.draftKey();
    if (!key) return;
    try {
      localStorage.removeItem(key);
    } catch {
      // Nothing to remove.
    }
  }

  private focusHeading(): void {
    setTimeout(() => document.querySelector<HTMLElement>('.step-panel h2')?.focus({ preventScroll: true }), 50);
  }
}

function emptyModel(): FormModel {
  return {
    title: { es: '', en: '' }, description: { es: '', en: '' }, defaultLanguage: 'es',
    resultsVisibility: 'CENSUS_ONLY', voterAuthentication: 'EMAIL_CODE', opensAt: '', closesAt: '', censusId: null, voteType: 'SINGLE_CHOICE',
    question: { es: '', en: '' }, options: [{ es: '', en: '' }, { es: '', en: '' }], blankVoteEnabled: true,
  };
}

function texts(text: LocalizedText | undefined): Record<Lang, string> {
  return { es: text?.es ?? '', en: text?.en ?? '' };
}

/** `datetime-local` works with the browser local time; the API works with instants. */
function toLocalInput(iso: string): string {
  const date = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function fromProcess(process: Process): FormModel {
  return {
    title: texts(process.title), description: texts(process.description), defaultLanguage: process.defaultLanguage,
    resultsVisibility: process.resultsVisibility, voterAuthentication: process.voterAuthentication ?? 'EMAIL_CODE',
    opensAt: toLocalInput(process.opensAt),
    closesAt: toLocalInput(process.closesAt), censusId: process.censusId, voteType: process.ballot.voteType,
    question: texts(process.ballot.question), blankVoteEnabled: process.ballot.blankVoteEnabled,
    options: process.ballot.voteType === 'SINGLE_CHOICE'
      ? process.ballot.options.map((option) => texts(option.text))
      : [{ es: '', en: '' }, { es: '', en: '' }],
  };
}

/**
 * Trims both languages; an empty language takes the text of the default language (RI-5), so a text written once is
 * saved in Spanish and English. An empty default language keeps the text as written (validation already requires it).
 */
function clean(text: Record<Lang, string>, def: Lang): LocalizedText {
  const result: LocalizedText = {};
  const fallback = text[def].trim();
  for (const lang of ['es', 'en'] as const) {
    const value = text[lang].trim() || fallback;
    if (value) result[lang] = value;
  }
  return result;
}

function toProcessData(m: FormModel): ProcessData {
  const def = m.defaultLanguage;
  return {
    censusId: m.censusId!, title: clean(m.title, def), description: clean(m.description, def),
    opensAt: new Date(m.opensAt).toISOString(), closesAt: new Date(m.closesAt).toISOString(),
    defaultLanguage: m.defaultLanguage, resultsVisibility: m.resultsVisibility,
    voterAuthentication: m.voterAuthentication,
    ballot: {
      voteType: m.voteType, question: clean(m.question, def), blankVoteEnabled: m.blankVoteEnabled,
      options: m.voteType === 'SINGLE_CHOICE'
        ? m.options.map((option, i) => ({ position: i + 1, text: clean(option, def) }))
        : [],
    },
  };
}
