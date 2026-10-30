import { Component, computed, inject, input, OnDestroy, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import {
  IonAlert, IonButton, IonIcon, IonInput, IonLabel, IonSearchbar, IonSegment, IonSegmentButton, IonSpinner, IonText,
} from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { ApiError, toApiError } from '../../../core/admin/api-error';
import {
  Census, CensusApiService, CSV_MAX_BYTES, ImportSummary, Voter, VoterData,
} from '../../../core/admin/census-api.service';
import { Lang } from '../../../core/admin/process-api.service';
import { AdminPageComponent } from '../admin-page.component';

/** Import of a CSV in two steps (D-27): validate (dry run, cancellable) → summary → apply. */
type ImportPhase = 'idle' | 'validating' | 'summary' | 'applying' | 'done';

type VoterField = 'fullName' | 'email' | 'phone' | 'institutionalId';

interface VoterForm {
  id: number | null;
  fullName: string;
  email: string;
  phone: string;
  institutionalId: string;
  preferredLanguage: Lang;
}

const PAGE_SIZE = 50;
const SHOWN_LINES = 20;
/** Same rules as `VoterData` in election-service (RI-3); the service has the last word. */
const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
const PHONE = /^[0-9]{1,15}$/;

/**
 * A2 (RF-03): one census. The census manager imports a CSV (summary before storing anything; G1 while it is being
 * validated), adds, edits and removes people while the census is not frozen; the administrator only reads it.
 */
@Component({
  selector: 'app-census-detail',
  imports: [
    AdminPageComponent, RouterLink, IonAlert, IonButton, IonIcon, IonInput, IonLabel, IonSearchbar, IonSegment,
    IonSegmentButton, IonSpinner, IonText, TranslatePipe,
  ],
  templateUrl: './census-detail.page.html',
  styleUrl: './census-detail.page.scss',
})
export class CensusDetailPage implements OnDestroy {
  readonly id = input.required<string>();

  private readonly api = inject(CensusApiService);
  private readonly translate = inject(TranslateService);
  protected readonly language = inject(LanguageService).current;
  private readonly isManager = inject(AuthService).hasAnyRole(['CENSUS_MANAGER']);

  protected readonly census = signal<Census | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  /** Changes only by the census manager and only while no SCHEDULED/OPEN process uses the census (D-21). */
  protected readonly canEdit = computed(() => this.isManager && this.census()?.frozen === false);
  protected readonly isManagerRole = this.isManager;

  // People of the census
  protected readonly voters = signal<Voter[]>([]);
  protected readonly total = signal(0);
  protected readonly page = signal(0);
  protected readonly search = signal('');
  protected readonly votersLoading = signal(false);
  protected readonly votersError = signal(false);
  protected readonly firstShown = computed(() => (this.total() === 0 ? 0 : this.page() * PAGE_SIZE + 1));
  protected readonly lastShown = computed(() => Math.min((this.page() + 1) * PAGE_SIZE, this.total()));
  protected readonly hasNext = computed(() => this.lastShown() < this.total());
  private searchTimer: ReturnType<typeof setTimeout> | undefined;

  // CSV import
  protected readonly phase = signal<ImportPhase>('idle');
  protected readonly file = signal<File | null>(null);
  protected readonly summary = signal<ImportSummary | null>(null);
  protected readonly importError = signal<string | null>(null);
  protected readonly imported = signal(0);
  protected readonly dragging = signal(false);
  protected readonly shownLines = SHOWN_LINES;
  private importSub: Subscription | undefined;

  // Manual add / edit
  protected readonly form = signal<VoterForm | null>(null);
  protected readonly formErrors = signal<Partial<Record<VoterField, string>>>({});
  protected readonly formServerError = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly fields: readonly VoterField[] = ['fullName', 'email', 'phone', 'institutionalId'];
  protected readonly maxLength: Record<VoterField, number> = { fullName: 60, email: 100, phone: 15, institutionalId: 50 };
  protected readonly notice = signal<string | null>(null);
  protected readonly pageError = signal<string | null>(null);

  // Removal
  protected readonly removing = signal<Voter | null>(null);
  protected readonly removeButtons = computed(() => {
    this.language();
    return [
      { text: this.translate.instant('admin.census.remove.cancel'), role: 'cancel' },
      { text: this.translate.instant('admin.census.remove.ok'), role: 'confirm' },
    ];
  });

  ionViewWillEnter(): void {
    void this.load();
  }

  ngOnDestroy(): void {
    this.importSub?.unsubscribe();
    clearTimeout(this.searchTimer);
  }

  private async load(): Promise<void> {
    try {
      this.census.set(await this.api.get(Number(this.id())));
      await this.loadVoters();
    } catch {
      this.loadError.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  private async refreshCensus(): Promise<void> {
    try {
      this.census.set(await this.api.get(Number(this.id())));
    } catch {
      // The list below still shows the latest data; the header keeps the previous count.
    }
  }

  protected async loadVoters(): Promise<void> {
    this.votersLoading.set(true);
    this.votersError.set(false);
    try {
      const result = await this.api.voters(Number(this.id()), this.page(), PAGE_SIZE, this.search().trim());
      this.voters.set(result.items);
      this.total.set(result.totalItems);
    } catch {
      this.votersError.set(true);
    } finally {
      this.votersLoading.set(false);
    }
  }

  protected onSearch(value: string | null | undefined): void {
    this.search.set(value ?? '');
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => {
      this.page.set(0);
      void this.loadVoters();
    }, 300);
  }

  protected goToPage(delta: number): void {
    this.page.update((page) => Math.max(0, page + delta));
    void this.loadVoters();
  }

  // ---- CSV import --------------------------------------------------------------------------------------------

  protected onFileInput(event: Event): void {
    const element = event.target as HTMLInputElement;
    const file = element.files?.[0];
    element.value = ''; // choosing the same file again after fixing it must trigger a new validation
    if (file) this.validate(file);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file && this.canEdit() && !this.busy()) this.validate(file);
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(this.canEdit() && !this.busy());
  }

  protected readonly busy = computed(() => this.phase() === 'validating' || this.phase() === 'applying');

  /** Client checks of D-27 (extension, size, not empty) and then the dry run of the service. */
  private validate(file: File): void {
    this.notice.set(null);
    this.summary.set(null);
    this.file.set(file);
    if (!/\.csv$/i.test(file.name)) return this.failImport('admin.census.import.errors.extension');
    if (file.size === 0) return this.failImport('admin.census.import.errors.empty');
    if (file.size > CSV_MAX_BYTES) return this.failImport('admin.census.import.errors.size');
    this.importError.set(null);
    this.phase.set('validating');
    this.importSub = this.api.importVoters(Number(this.id()), file, true).subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.phase.set('summary');
      },
      error: (error) => this.importFailed(error),
    });
  }

  /** G1: a dry run stores nothing, so it can be cancelled safely by aborting the request. */
  protected cancelValidation(): void {
    this.importSub?.unsubscribe();
    this.resetImport();
  }

  protected resetImport(): void {
    this.phase.set('idle');
    this.file.set(null);
    this.summary.set(null);
    this.importError.set(null);
  }

  /**
   * Applies the import. It is not cancellable (the request may already be stored); `discardInvalidLines` is the
   * explicit decision of D-27 to leave out the lines with errors.
   */
  protected apply(discardInvalidLines: boolean): void {
    const file = this.file();
    if (!file) return;
    this.importError.set(null);
    this.phase.set('applying');
    this.importSub = this.api.importVoters(Number(this.id()), file, false, discardInvalidLines).subscribe({
      next: async (summary) => {
        if (!summary.applied) {
          // The census changed since the dry run (e.g. new invalid lines): show the new summary again.
          this.summary.set(summary);
          this.phase.set('summary');
          this.importError.set(this.translate.instant('admin.census.import.notApplied'));
          return;
        }
        this.imported.set(summary.toCreate);
        this.phase.set('done');
        this.page.set(0);
        await Promise.all([this.refreshCensus(), this.loadVoters()]);
      },
      error: (error) => this.importFailed(error),
    });
  }

  private failImport(key: string): void {
    this.importError.set(this.translate.instant(key, { max: CSV_MAX_BYTES / 1024 / 1024 }));
    this.phase.set('idle');
  }

  private importFailed(error: unknown): void {
    const apiError = toApiError(error);
    if (apiError instanceof ApiError && apiError.status === 413) {
      this.failImport('admin.census.import.errors.size');
    } else {
      this.importError.set(apiError instanceof ApiError && apiError.detail ? apiError.detail : this.translate.instant('common.loadError'));
      this.phase.set(this.summary() ? 'summary' : 'idle');
    }
    if (apiError instanceof ApiError && apiError.status === 409) void this.refreshCensus(); // frozen meanwhile
  }

  protected reasonKey(reason: string): string {
    return 'admin.census.import.reason.' + reason;
  }

  // ---- Manual add / edit -------------------------------------------------------------------------------------

  protected openNew(): void {
    this.notice.set(null);
    this.form.set({ id: null, fullName: '', email: '', phone: '', institutionalId: '', preferredLanguage: 'es' });
    this.formErrors.set({});
    this.formServerError.set(null);
  }

  protected openEdit(voter: Voter): void {
    this.notice.set(null);
    this.form.set({
      id: voter.id, fullName: voter.fullName, email: voter.email, phone: voter.phone ?? '',
      institutionalId: voter.institutionalId ?? '', preferredLanguage: voter.preferredLanguage ?? 'es',
    });
    this.formErrors.set({});
    this.formServerError.set(null);
  }

  protected closeForm(): void {
    this.form.set(null);
  }

  protected setField(field: keyof VoterForm, value: string | null | undefined): void {
    this.form.update((form) => (form ? { ...form, [field]: value ?? '' } : form));
    if (field in this.formErrors()) {
      this.formErrors.update((errors) => {
        const next = { ...errors };
        delete next[field as VoterField];
        return next;
      });
    }
  }

  private validateForm(form: VoterForm): Partial<Record<VoterField, string>> {
    const errors: Partial<Record<VoterField, string>> = {};
    const t = (key: string) => this.translate.instant('admin.census.form.errors.' + key);
    const name = form.fullName.trim();
    if (!name || name.length > 60) errors.fullName = t('fullName');
    const email = form.email.trim();
    if (email.length > 100 || !EMAIL.test(email)) errors.email = t('email');
    if (form.phone.trim() && !PHONE.test(form.phone.trim())) errors.phone = t('phone');
    if (form.institutionalId.trim().length > 50) errors.institutionalId = t('institutionalId');
    return errors;
  }

  protected async save(): Promise<void> {
    const form = this.form();
    if (!form) return;
    const errors = this.validateForm(form);
    this.formErrors.set(errors);
    this.formServerError.set(null);
    if (Object.keys(errors).length) return;
    const data: VoterData = {
      fullName: form.fullName.trim(),
      email: form.email.trim(),
      phone: form.phone.trim() || null,
      institutionalId: form.institutionalId.trim() || null,
      preferredLanguage: form.preferredLanguage,
    };
    this.saving.set(true);
    try {
      const id = Number(this.id());
      const voter = form.id === null ? await this.api.addVoter(id, data) : await this.api.updateVoter(id, form.id, data);
      this.form.set(null);
      this.notice.set(this.translate.instant(form.id === null ? 'admin.census.form.added' : 'admin.census.form.updated', { name: voter.fullName }));
      await Promise.all([this.refreshCensus(), this.loadVoters()]);
    } catch (error) {
      if (error instanceof ApiError) {
        const field = error.errors[0]?.field === 'language' ? null : (error.errors[0]?.field as VoterField | undefined);
        if (field && ['fullName', 'email', 'phone', 'institutionalId'].includes(field)) {
          this.formErrors.set({ [field]: error.errors[0].message });
        } else {
          this.formServerError.set(error.detail ?? this.translate.instant('common.loadError'));
        }
        if (error.status === 409) void this.refreshCensus();
      } else {
        this.formServerError.set(this.translate.instant('common.loadError'));
      }
    } finally {
      this.saving.set(false);
    }
  }

  // ---- Removal -----------------------------------------------------------------------------------------------

  protected async onRemoveDismiss(role: string | undefined): Promise<void> {
    const voter = this.removing();
    this.removing.set(null);
    if (role !== 'confirm' || !voter) return;
    this.notice.set(null);
    this.pageError.set(null);
    try {
      await this.api.removeVoter(Number(this.id()), voter.id);
      this.notice.set(this.translate.instant('admin.census.remove.done', { name: voter.fullName }));
      if (this.voters().length === 1 && this.page() > 0) this.page.update((page) => page - 1);
      await Promise.all([this.refreshCensus(), this.loadVoters()]);
    } catch (error) {
      this.pageError.set(error instanceof ApiError && error.detail ? error.detail : this.translate.instant('common.loadError'));
      if (error instanceof ApiError && error.status === 409) void this.refreshCensus();
    }
  }
}
