import { Component, OnDestroy, computed, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { IonButton, IonIcon, IonSpinner, IonText } from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { Process, ProcessApiService, ProcessStatus } from '../../../core/admin/process-api.service';
import { Result, ResultsApiService } from '../../../core/admin/results-api.service';
import { verifyResult } from '../../../core/results/result-verifier';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { LocalizedPipe } from '../../../shared/pipes/localized.pipe';
import { ResultBarsComponent } from '../../../shared/result-bars/result-bars.component';
import {
  ResultVerificationComponent, VerificationState,
} from '../../../shared/result-verification/result-verification.component';
import { StatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { AdminPageComponent } from '../admin-page.component';
import { ProcessPickerComponent } from '../process-picker/process-picker.component';

const POLL_MS = 5_000;

/**
 * F1 (RF-15) and F4 (RF-13): result of a closed process for ADMIN and AUDITOR. While the ballot box is still counting,
 * F4 waits and asks again every 5 s. The result is verified in the browser with the published counted votes (D-37);
 * the CSV is only for ADMIN.
 */
@Component({
  selector: 'app-results',
  imports: [
    AdminPageComponent, ProcessPickerComponent, IonButton, IonIcon, IonSpinner, IonText, TranslatePipe, LocalDatePipe,
    LocalizedPipe, ResultBarsComponent, ResultVerificationComponent, StatusBadgeComponent,
  ],
  template: `
    <app-admin-page titleKey="admin.results.title">
      <div class="top">
        <app-process-picker [statuses]="statuses" [selected]="selectedId()" (selectedChange)="select($event)"
                            emptyKey="admin.results.empty" />
      </div>
      @if (loading()) {
        <ion-spinner role="status" [attr.aria-label]="'common.loading' | translate" />
      } @else if (error()) {
        <ion-text color="danger"><p role="alert">{{ 'common.loadError' | translate }}</p></ion-text>
      } @else if (process(); as p) {
        <div class="head">
          <div>
            <h2 class="name">{{ p.title | localized: language() : p.defaultLanguage }}</h2>
            <p class="meta">
              <app-status-badge [status]="p.status" />
              {{ 'admin.results.closedOn' | translate: { date: (p.closesAt | localDate: language()) } }} ·
              {{ 'admin.process.voteType.' + p.ballot.voteType | translate }}
            </p>
          </div>
          @if (result() && canExport) {
            <ion-button fill="outline" (click)="exportCsv()" [disabled]="exporting()">
              <ion-icon slot="start" name="download-outline" aria-hidden="true"></ion-icon>
              {{ 'admin.results.export' | translate }}</ion-button>
          }
        </div>
        @if (exportError()) { <p class="server-error" role="alert">{{ exportError() }}</p> }

        @if (!result()) {
          <!-- F4 -->
          <section class="card tallying" role="status">
            <ion-spinner name="crescent" aria-hidden="true"></ion-spinner>
            <h3>{{ 'admin.results.tallyingTitle' | translate }}</h3>
            <p>{{ 'admin.results.tallyingText' | translate }}</p>
            <ul class="steps">
              <li>{{ 'admin.results.tallyingStep1' | translate }}</li>
              <li>{{ 'admin.results.tallyingStep2' | translate }}</li>
              <li>{{ 'admin.results.tallyingStep3' | translate }}</li>
            </ul>
          </section>
        } @else {
          @let r = result()!;
          <div class="metrics">
            <section class="card metric"><span>{{ 'admin.results.census' | translate }}</span>
              <strong>{{ n(r.eligibleVoters) }}</strong><small>{{ 'admin.results.people' | translate }}</small></section>
            <section class="card metric"><span>{{ 'admin.results.votes' | translate }}</span>
              <strong>{{ n(r.totalVotes) }}</strong>
              <small>{{ 'admin.results.turnout' | translate: { pct: pct(r.participation) } }}</small></section>
            @if (r.blankVoteEnabled) {
              <section class="card metric"><span>{{ 'admin.results.blank' | translate }}</span>
                <strong>{{ n(r.blankVotes) }}</strong>
                <small>{{ 'admin.results.ofVotes' | translate: { pct: pct(r.blankPercentage) } }}</small></section>
            }
          </div>
          <section class="card">
            <h3>{{ 'admin.results.byOption' | translate }}</h3>
            <p class="question">{{ r.question | localized: language() : p.defaultLanguage }}</p>
            <app-result-bars [result]="r" [language]="language()" />
          </section>
          <div class="grid">
            <app-result-verification [state]="verification()" [resultHash]="r.resultHash" [finalHash]="r.finalHash" />
            <section class="card side">
              @if (publicLink(); as link) {
                <h3>{{ 'admin.results.publicTitle' | translate }}</h3>
                <p class="help">{{ 'admin.results.publicText' | translate }}</p>
                <p class="mono link">{{ link }}</p>
                <ion-button fill="outline" size="small" (click)="copy(link)">
                  <ion-icon slot="start" name="copy-outline" aria-hidden="true"></ion-icon>
                  {{ (copied() ? 'admin.results.copied' : 'admin.results.copy') | translate }}</ion-button>
              } @else {
                <h3>{{ 'admin.results.censusOnlyTitle' | translate }}</h3>
                <p class="help">{{ 'admin.results.censusOnlyText' | translate }}</p>
              }
              <p class="help">{{ 'admin.results.talliedAt' | translate: { date: (r.talliedAt | localDate: language()) } }}</p>
            </section>
          </div>
        }
      }
    </app-admin-page>
  `,
  styles: `
    .top { display: flex; justify-content: flex-end; margin-bottom: 8px; }
    .head { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
    .name { font-size: 1.5rem; margin: 0; }
    @media (min-width: 768px) { .name { font-size: 1.75rem; } }
    .meta { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; color: var(--ion-color-medium); margin: 6px 0 0; }
    .metrics { display: grid; gap: 16px; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); margin-bottom: 16px; }
    .metrics .card { margin: 0; }
    .metric { display: flex; flex-direction: column; gap: 2px; }
    .metric span { font-size: 0.875rem; color: #4a5868; }
    .metric strong { font-size: 2rem; color: var(--ion-color-primary); line-height: 1.1; }
    .metric small { color: var(--ion-color-medium); }
    .question { color: #4a5868; margin: -4px 0 16px; }
    .grid { display: grid; gap: 16px; grid-template-columns: 1fr; }
    @media (min-width: 1000px) { .grid { grid-template-columns: 3fr 2fr; align-items: start; } .grid .card { margin: 0; } }
    .help { color: var(--ion-color-medium); font-size: 0.875rem; }
    .link { overflow-wrap: anywhere; font-size: 0.8125rem; background: #f2f4f7; padding: 8px; border-radius: 6px; }
    .side ion-button { margin: 0 0 8px; }
    .tallying { text-align: center; max-width: 640px; margin: 24px auto; padding: 32px 24px; }
    .tallying ion-spinner { width: 48px; height: 48px; color: var(--ion-color-primary); }
    .tallying h3 { font-size: 1.25rem; margin: 12px 0 4px; }
    .steps { text-align: left; display: inline-block; margin: 12px 0 0; color: #4a5868; line-height: 1.8; }
  `,
})
export class ResultsPage implements OnDestroy {
  readonly proceso = input<string | undefined>();

  private readonly processes = inject(ProcessApiService);
  private readonly api = inject(ResultsApiService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  protected readonly language = inject(LanguageService).current;
  protected readonly canExport = inject(AuthService).hasAnyRole(['ADMIN']);
  protected readonly statuses: readonly ProcessStatus[] = ['CLOSED', 'TALLIED', 'ARCHIVED'];
  protected readonly selectedId = computed(() => (this.proceso() ? Number(this.proceso()) : null));
  protected readonly process = signal<Process | null>(null);
  protected readonly result = signal<Result | null>(null);
  protected readonly verification = signal<VerificationState>('checking');
  protected readonly loading = signal(false);
  protected readonly error = signal(false);
  protected readonly exporting = signal(false);
  protected readonly exportError = signal<string | null>(null);
  protected readonly copied = signal(false);
  protected readonly publicLink = computed(() => {
    const id = this.process()?.resultsPublicId;
    return id ? `${location.origin}/resultados/${id}` : null;
  });
  private poll: ReturnType<typeof setTimeout> | undefined;

  ionViewWillEnter(): void {
    if (this.selectedId()) void this.load(true);
  }

  ionViewWillLeave(): void {
    clearTimeout(this.poll);
  }

  ngOnDestroy(): void {
    clearTimeout(this.poll);
  }

  protected async select(id: number): Promise<void> {
    await this.router.navigate([], { queryParams: { proceso: id }, replaceUrl: true });
    await this.load(true);
  }

  private async load(showSpinner: boolean): Promise<void> {
    clearTimeout(this.poll);
    const id = this.selectedId();
    if (!id) return;
    if (showSpinner) {
      this.loading.set(true);
      this.result.set(null);
    }
    try {
      const [process, result] = await Promise.all([this.processes.get(id), this.api.result(id)]);
      if (this.selectedId() !== id) return;
      this.process.set(process);
      this.result.set(result);
      this.error.set(false);
      if (result) {
        void this.verify(id, result);
      } else {
        // F4: the tally runs on its own a few seconds after closing.
        this.poll = setTimeout(() => void this.load(false), POLL_MS);
      }
    } catch {
      this.error.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  private async verify(id: number, result: Result): Promise<void> {
    this.verification.set('checking');
    try {
      const verification = await verifyResult(await this.api.countedVotes(id), result);
      this.verification.set(verification.matches ? 'ok' : 'mismatch');
    } catch {
      this.verification.set('error');
    }
  }

  protected async exportCsv(): Promise<void> {
    const id = this.selectedId();
    if (!id) return;
    this.exporting.set(true);
    this.exportError.set(null);
    try {
      const blob = await this.api.exportCsv(id, this.language());
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${this.translate.instant('admin.results.fileName')}-${id}.csv`;
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch {
      this.exportError.set(this.translate.instant('admin.results.exportError'));
    } finally {
      this.exporting.set(false);
    }
  }

  protected async copy(text: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(text);
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2500);
    } catch {
      // Clipboard not allowed: the link stays visible to copy by hand.
    }
  }

  protected n(value: number): string {
    return new Intl.NumberFormat(this.language()).format(value);
  }

  protected pct(value: number): string {
    return new Intl.NumberFormat(this.language(), { maximumFractionDigits: 2 }).format(+value);
  }
}
