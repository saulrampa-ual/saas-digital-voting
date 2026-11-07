import { Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonAlert, IonButton, IonIcon, IonSpinner, IonText } from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';
import { LanguageService } from '../../../core/i18n/language.service';
import {
  ApiError, CensusSummary, FieldError, Lang, PROCESS_STATUSES, Process, ProcessApiService, ProcessStatus, StatusChange,
} from '../../../core/admin/process-api.service';
import { LocalizedPipe } from '../../../shared/pipes/localized.pipe';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { StatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { AdminPageComponent } from '../admin-page.component';

interface Check {
  key: string;
  ok: boolean;
  params?: Record<string, unknown>;
}

/**
 * A4 (RF-07): status of a process, available actions and history. Opening and closing are automatic; the
 * administrator schedules a complete DRAFT (the census is frozen and invitations are sent) and archives a TALLIED
 * process. The pre-scheduling checklist mirrors the service rules, which have the last word (422).
 */
@Component({
  selector: 'app-process-detail',
  imports: [
    AdminPageComponent, RouterLink, IonAlert, IonButton, IonIcon, IonSpinner, IonText, TranslatePipe, LocalizedPipe, LocalDatePipe,
    StatusBadgeComponent,
  ],
  templateUrl: './process-detail.page.html',
  styleUrl: './process-detail.page.scss',
})
export class ProcessDetailPage {
  readonly id = input.required<string>();

  private readonly api = inject(ProcessApiService);
  private readonly translate = inject(TranslateService);
  protected readonly language = inject(LanguageService).current;
  private readonly auth = inject(AuthService);
  protected readonly canManage = this.auth.hasAnyRole(['ADMIN']);
  protected readonly isAuditor = this.auth.hasAnyRole(['AUDITOR']);
  /** RF-14 (ADMIN), RF-15 (ADMIN, AUDITOR) and RF-12 (AUDITOR) pages of this process, by status. */
  protected readonly links = computed(() => {
    const status = this.process()?.status;
    if (!status || status === 'DRAFT') return [];
    const closed = ['CLOSED', 'TALLIED', 'ARCHIVED'].includes(status);
    return [
      ...(this.canManage ? [{ path: '/admin/participacion', key: 'admin.process.links.participation', icon: 'bar-chart-outline' }] : []),
      ...(closed ? [{ path: '/admin/resultados', key: 'admin.process.links.results', icon: 'document-text-outline' }] : []),
      ...(this.isAuditor ? [{ path: '/admin/auditoria', key: 'admin.process.links.audit', icon: 'shield-checkmark-outline' }] : []),
    ];
  });
  protected readonly statuses = PROCESS_STATUSES;

  protected readonly process = signal<Process | null>(null);
  protected readonly census = signal<CensusSummary | null>(null);
  protected readonly history = signal<StatusChange[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly actionError = signal<string | null>(null);
  protected readonly issues = signal<FieldError[]>([]);
  protected readonly notice = signal<string | null>(null);

  /** The pre-scheduling checklist only makes sense for a DRAFT that this user can schedule. */
  protected readonly showChecks = computed(() => this.canManage && this.process()?.status === 'DRAFT');
  /** Edit and schedule exist only in DRAFT, archive only in TALLIED (RF-07); other statuses have no manual action. */
  protected readonly hasActions = computed(() => ['DRAFT', 'TALLIED'].includes(this.process()?.status ?? ''));

  protected readonly currentIndex = computed(() => PROCESS_STATUSES.indexOf(this.process()?.status ?? 'DRAFT'));

  /** Client-side view of RF-07 step 2 (opening in the future, census with voters, every text in es and en). */
  protected readonly checks = computed<Check[]>(() => {
    const p = this.process();
    if (!p) return [];
    const both = (text: Partial<Record<Lang, string>>) => !!text.es && !!text.en;
    const hasDescription = !!p.description.es || !!p.description.en;
    return [
      { key: 'texts', ok: both(p.title) && (!hasDescription || both(p.description)) },
      { key: 'calendar', ok: new Date(p.opensAt) > new Date() && new Date(p.closesAt) > new Date(p.opensAt) },
      { key: 'census', ok: (this.census()?.voterCount ?? 0) > 0, params: { name: this.census()?.name ?? '—', count: this.census()?.voterCount ?? 0 } },
      { key: 'ballot', ok: both(p.ballot.question) && p.ballot.options.every((o) => both(o.text)) },
    ];
  });

  ionViewWillEnter(): void {
    void this.load();
  }

  private async load(): Promise<void> {
    try {
      const id = Number(this.id());
      const [process, history, censuses] = await Promise.all([
        // Censuses are readable by ADMIN and CENSUS_MANAGER only (an auditor never sees voter data, RNF-16).
        this.api.get(id), this.api.history(id), this.canManage ? this.api.censuses().catch(() => []) : Promise.resolve([]),
      ]);
      this.process.set(process);
      this.history.set(history);
      this.census.set(censuses.find((c) => c.id === process.censusId) ?? null);
    } catch {
      this.loadError.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  protected dateOf(status: ProcessStatus): string | null {
    const p = this.process();
    if (!p) return null;
    if (status === 'OPEN') return p.opensAt;
    if (status === 'CLOSED') return p.closesAt;
    return this.history().find((change) => change.to === status)?.changedAt ?? null;
  }

  /** Confirmation dialog of «Programar» (declarative ion-alert: the overlay controller would weigh on main). */
  protected readonly confirming = signal(false);
  protected readonly confirmButtons = computed(() => {
    this.language();
    return [
      { text: this.translate.instant('admin.process.scheduleConfirm.cancel'), role: 'cancel' },
      { text: this.translate.instant('admin.process.scheduleConfirm.ok'), role: 'confirm' },
    ];
  });

  protected async onConfirmDismiss(role: string | undefined): Promise<void> {
    this.confirming.set(false);
    if (role === 'confirm') {
      await this.run(() => this.api.schedule(Number(this.id())), 'admin.process.scheduled');
    }
  }

  protected async archive(): Promise<void> {
    await this.run(() => this.api.archive(Number(this.id())), 'admin.process.archived');
  }

  private async run(action: () => Promise<Process>, noticeKey: string): Promise<void> {
    this.busy.set(true);
    this.actionError.set(null);
    this.issues.set([]);
    this.notice.set(null);
    try {
      this.process.set(await action());
      this.history.set(await this.api.history(Number(this.id())));
      this.notice.set(this.translate.instant(noticeKey));
    } catch (error) {
      if (error instanceof ApiError) {
        this.actionError.set(error.detail);
        this.issues.set(error.errors);
      } else {
        this.actionError.set(this.translate.instant('common.loadError'));
      }
    } finally {
      this.busy.set(false);
    }
  }
}
