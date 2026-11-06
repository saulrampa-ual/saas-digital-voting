import { Component, OnDestroy, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { IonIcon, IonSpinner, IonText } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../core/i18n/language.service';
import { Participation, ResultsApiService } from '../../../core/admin/results-api.service';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { ProcessStatus } from '../../../core/admin/process-api.service';
import { AdminPageComponent } from '../admin-page.component';
import { ProcessPickerComponent } from '../process-picker/process-picker.component';
import { NotificationsPanelComponent } from './notifications-panel.component';
import { DayChartComponent, DayChartPoint } from '../../../shared/day-chart/day-chart.component';

const REFRESH_MS = 15_000;

/**
 * A5 (RF-14): how many people have voted, never what. Refreshes itself every 15 s while the process is open.
 * Votes per day only (D-18): the service does not know the time of any vote. Below, the reminders and emails of the
 * process (RF-17, D-45).
 */
@Component({
  selector: 'app-participation',
  imports: [AdminPageComponent, ProcessPickerComponent, RouterLink, IonIcon, IonSpinner, IonText, TranslatePipe,
    LocalDatePipe, NotificationsPanelComponent, DayChartComponent],
  template: `
    <app-admin-page titleKey="admin.participation.title" shortTitleKey="admin.participation.titleShort">
      <div class="top">
        <app-process-picker [statuses]="statuses" [selected]="selectedId()" (selectedChange)="select($event)" [showCurrent]="true"
                            emptyKey="admin.participation.empty" />
      </div>
      @if (loading()) {
        <ion-spinner role="status" [attr.aria-label]="'common.loading' | translate" />
      } @else if (error()) {
        <ion-text color="danger"><p role="alert">{{ 'common.loadError' | translate }}</p></ion-text>
      } @else if (data(); as d) {
        <p class="meta">
          @if (d.status === 'SCHEDULED') { {{ 'admin.participation.opens' | translate: { date: (d.opensAt | localDate: language()) } }} }
          @else { {{ 'admin.participation.closes' | translate: { date: (d.closesAt | localDate: language()) } }} }
        </p>
        @if (d.status === 'OPEN') {
          <p class="live" role="status"><ion-icon name="refresh-outline" aria-hidden="true"></ion-icon>
            {{ 'admin.participation.live' | translate: { seconds: secondsAgo() } }}</p>
        }
        <div class="metrics">
          <section class="card ring-card">
            <h3>{{ 'admin.participation.current' | translate }}</h3>
            <div class="ring" role="img" [attr.aria-label]="'admin.participation.ringLabel' | translate: { pct: pct(d) }"
                 [style.--pct]="+d.participation">
              <span class="ring-value">{{ pct(d) }} %</span><span class="ring-sub">{{ 'admin.participation.ofCensus' | translate }}</span>
            </div>
          </section>
          <section class="card metric"><span>{{ 'admin.participation.census' | translate }}</span>
            <strong>{{ n(d.eligibleVoters) }}</strong><small>{{ 'admin.participation.censusHelp' | translate }}</small></section>
          <section class="card metric"><span>{{ 'admin.participation.votes' | translate }}</span>
            <strong>{{ n(d.votesCast) }}</strong><small>{{ 'admin.participation.votesHelp' | translate }}</small></section>
          <section class="card metric"><span>{{ 'admin.participation.pending' | translate }}</span>
            <strong>{{ n(d.pending) }}</strong><small>{{ 'admin.participation.pendingHelp' | translate }}</small></section>
        </div>

        <section class="card">
          <h3 id="by-day">{{ 'admin.participation.byDayTitle' | translate }}</h3>
          @if (d.status === 'SCHEDULED') {
            <p class="help">{{ 'admin.participation.notOpen' | translate }}</p>
          } @else if (d.byDay.length === 0) {
            <p class="help">{{ 'admin.participation.noVotes' | translate }}</p>
          } @else {
            <app-day-chart [points]="chartPoints()" [from]="d.opensAt" [to]="d.closesAt" [language]="language()" />
            <table class="sr-only">
              <caption>{{ 'admin.participation.byDayTitle' | translate }}</caption>
              <tr><th scope="col">{{ 'admin.participation.day' | translate }}</th><th scope="col">{{ 'admin.participation.votes' | translate }}</th>
                <th scope="col">{{ 'admin.participation.cumulative' | translate }}</th></tr>
              @for (day of d.byDay; track day.day) {
                <tr><td>{{ day.day | localDate: language() : false }}</td><td>{{ day.votes }}</td><td>{{ day.cumulative }}</td></tr>
              }
            </table>
          }
        </section>

        <app-notifications-panel [processId]="d.processId" [pending]="d.pending" [version]="loadedAt()" />

        <p class="info-box lock"><ion-icon name="lock-closed-outline" aria-hidden="true"></ion-icon>
          <span><strong>{{ 'admin.participation.lockTitle' | translate }}</strong> {{ 'admin.participation.lockText' | translate }}</span></p>
        @if (d.status === 'TALLIED' || d.status === 'ARCHIVED') {
          <a class="action-link" [routerLink]="['/admin/resultados']" [queryParams]="{ proceso: d.processId }">
            <ion-icon name="document-text-outline" aria-hidden="true"></ion-icon>{{ 'admin.participation.seeResults' | translate }}</a>
        }
      }
    </app-admin-page>
  `,
  styles: `
    .top { margin-bottom: 12px; }
    .meta { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; color: var(--ion-color-medium); margin: 0 0 4px; }
    .live { display: flex; gap: 6px; align-items: center; color: var(--ion-color-success); font-weight: 600; font-size: 0.875rem; margin: 0 0 12px; }
    .metrics { display: grid; gap: 16px; grid-template-columns: 1fr 1fr; margin-bottom: 16px; }
    @media (min-width: 1000px) { .metrics { grid-template-columns: 1.4fr 1fr 1fr 1fr; } }
    .metrics .card { margin: 0; }
    .ring-card { grid-column: 1 / -1; }
    @media (min-width: 1000px) { .ring-card { grid-column: auto; } }
    .ring { --pct: 0; width: 150px; height: 150px; border-radius: 50%; margin: 0 auto; display: grid; place-content: center; text-align: center;
      background: radial-gradient(closest-side, #fff 78%, transparent 79%),
        conic-gradient(var(--ion-color-primary) calc(var(--pct) * 1%), #e3e8ef 0); }
    .ring-value { font-size: 2rem; font-weight: 700; color: var(--ion-color-primary); }
    .ring-sub { font-size: 0.875rem; color: var(--ion-color-medium); }
    .metric { display: flex; flex-direction: column; justify-content: center; gap: 2px; }
    .metric span { font-size: 0.875rem; color: #4a5868; }
    .metric strong { font-size: 2rem; color: var(--ion-color-primary); line-height: 1.1; }
    .metric small { color: var(--ion-color-medium); }
    .help { color: var(--ion-color-medium); font-size: 0.875rem; }
    .lock { display: flex; gap: 8px; align-items: flex-start; margin-top: 24px; }
    .lock ion-icon { flex: none; margin-top: 3px; }
    .action-link { margin-top: 12px; }
  `,
})
export class ParticipationPage implements OnDestroy {
  /** Query parameter `?proceso=` (component input binding). */
  readonly proceso = input<string | undefined>();

  private readonly api = inject(ResultsApiService);
  private readonly router = inject(Router);
  protected readonly language = inject(LanguageService).current;
  protected readonly statuses: readonly ProcessStatus[] = ['SCHEDULED', 'OPEN', 'CLOSED', 'TALLIED', 'ARCHIVED'];
  protected readonly selectedId = computed(() => (this.proceso() ? Number(this.proceso()) : null));
  protected readonly data = signal<Participation | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal(false);
  protected readonly loadedAt = signal(0);
  private readonly now = signal(Date.now());
  protected readonly secondsAgo = computed(() => Math.max(0, Math.round((this.now() - this.loadedAt()) / 1000)));
  /** Cumulative share of the census per day, for the chart (people only, D-18). */
  protected readonly chartPoints = computed<DayChartPoint[]>(() => {
    const d = this.data();
    if (!d) return [];
    return d.byDay.map((day) => ({ day: day.day, cumulative: day.cumulative, pct: this.ratio(day.cumulative, d.eligibleVoters) }));
  });
  private timer: ReturnType<typeof setInterval> | undefined;
  private ticker: ReturnType<typeof setInterval> | undefined;

  ionViewWillEnter(): void {
    if (this.selectedId()) void this.load(true);
    this.ticker = setInterval(() => this.now.set(Date.now()), 1000);
    this.timer = setInterval(() => {
      if (this.data()?.status === 'OPEN') void this.load(false);
    }, REFRESH_MS);
  }

  ionViewWillLeave(): void {
    this.stop();
  }

  ngOnDestroy(): void {
    this.stop();
  }

  private stop(): void {
    clearInterval(this.timer);
    clearInterval(this.ticker);
  }

  protected async select(id: number): Promise<void> {
    await this.router.navigate([], { queryParams: { proceso: id }, replaceUrl: true });
    await this.load(true);
  }

  private async load(showSpinner: boolean): Promise<void> {
    const id = this.selectedId();
    if (!id) return;
    if (showSpinner) this.loading.set(true);
    try {
      this.data.set(await this.api.participation(id));
      this.error.set(false);
      this.loadedAt.set(Date.now());
    } catch {
      this.error.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  protected pct(d: Participation): string {
    return this.format(+d.participation);
  }

  protected share(part: number, total: number): string {
    return this.format(total ? Math.round((part * 10000) / total) / 100 : 0);
  }

  protected ratio(part: number, total: number): number {
    return total ? Math.min(100, (part * 100) / total) : 0;
  }

  protected n(value: number): string {
    return new Intl.NumberFormat(this.language()).format(value);
  }

  private format(value: number): string {
    return new Intl.NumberFormat(this.language(), { maximumFractionDigits: 1 }).format(value);
  }
}
