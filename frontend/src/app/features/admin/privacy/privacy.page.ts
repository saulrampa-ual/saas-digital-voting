import { Component, computed, inject, signal } from '@angular/core';
import { IonButton, IonIcon, IonSpinner } from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ApiError } from '../../../core/admin/api-error';
import { PrivacyApiService, PrivacySubject } from '../../../core/admin/privacy-api.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { LocalizedPipe } from '../../../shared/pipes/localized.pipe';
import { StatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { AdminPageComponent } from '../admin-page.component';

/**
 * F2 (RF-18, D-46): the ADMIN finds a person of the censuses by email or phone, exports their data and erases them.
 * Unlike the prototype it never says whether the person voted: erasure anonymizes every census a process used and
 * deletes the rest, the same for everybody. The vote is never part of it (RNF-11).
 */
@Component({
  selector: 'app-privacy',
  imports: [AdminPageComponent, IonButton, IonIcon, IonSpinner, TranslatePipe, LocalDatePipe, LocalizedPipe,
    StatusBadgeComponent],
  template: `
    <app-admin-page titleKey="admin.privacy.title" shortTitleKey="admin.privacy.titleShort">
      <p class="intro">{{ 'admin.privacy.intro' | translate }}</p>

      <section class="card" aria-labelledby="step1">
        <h3 id="step1">{{ 'admin.privacy.step1' | translate }}</h3>
        <form class="find" novalidate (submit)="$event.preventDefault(); find()">
          <label>{{ 'admin.privacy.queryLabel' | translate }}
            <input type="text" autocomplete="off" inputmode="email" [value]="query()" maxlength="100"
                   (input)="query.set($any($event.target).value)" [attr.aria-describedby]="findError() ? 'find-error' : 'find-help'" /></label>
          <ion-button type="submit" [disabled]="busy() || !query().trim()">
            <ion-icon slot="start" name="search-outline" aria-hidden="true"></ion-icon>
            {{ 'admin.privacy.find' | translate }}</ion-button>
        </form>
        <p id="find-help" class="help">{{ 'admin.privacy.queryHelp' | translate }}</p>
        @if (findError(); as e) { <p id="find-error" class="server-error" role="alert">{{ e }}</p> }
        @if (done(); as d) { <p class="ok" role="status">{{ d }}</p> }
      </section>

      @if (busy() && !subject()) {
        <ion-spinner role="status" [attr.aria-label]="'common.loading' | translate" />
      }

      @if (subject(); as s) {
        <section class="card" aria-labelledby="step2">
          <h3 id="step2">{{ 'admin.privacy.step2' | translate }}</h3>
          <dl class="facts">
            <div><dt>{{ 'admin.privacy.name' | translate }}</dt><dd>{{ s.fullName }}</dd></div>
            <div><dt>{{ 'admin.privacy.censuses' | translate }}</dt><dd>{{ s.entries.length }}</dd></div>
            <div><dt>{{ 'admin.privacy.processes' | translate }}</dt><dd>{{ processCount() }}</dd></div>
          </dl>
          <!-- DEF-07 (WCAG 2.1.1): scrollable table as a focusable region with a name -->
          <div class="table-wrap" tabindex="0" role="region" aria-labelledby="step2">
            <table>
              <caption class="sr-only">{{ 'admin.privacy.step2' | translate }}</caption>
              <thead><tr>
                <th scope="col">{{ 'admin.privacy.col.census' | translate }}</th>
                <th scope="col">{{ 'admin.privacy.col.contact' | translate }}</th>
                <th scope="col">{{ 'admin.privacy.col.added' | translate }}</th>
                <th scope="col">{{ 'admin.privacy.col.processes' | translate }}</th>
              </tr></thead>
              <tbody>
                @for (e of s.entries; track $index) {
                  <tr>
                    <td [attr.data-label]="'admin.privacy.col.census' | translate"><strong>{{ e.census }}</strong><br /><small>{{ e.fullName }}</small></td>
                    <td [attr.data-label]="'admin.privacy.col.contact' | translate">{{ e.email }}
                      @if (e.phone) { <br /><small>{{ e.phone }}</small> }
                      @if (e.institutionalId) { <br /><small>{{ 'admin.privacy.institutionalId' | translate }}: {{ e.institutionalId }}</small> }</td>
                    <td [attr.data-label]="'admin.privacy.col.added' | translate">{{ e.addedAt | localDate: language() : false }}</td>
                    <td [attr.data-label]="'admin.privacy.col.processes' | translate">
                      @for (p of e.processes; track p.id) {
                        <span class="process">{{ p.title | localized: language() }} <app-status-badge [status]="p.status" /></span>
                      } @empty { <small>{{ 'admin.privacy.noProcesses' | translate }}</small> }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <p class="help"><ion-icon name="lock-closed-outline" aria-hidden="true"></ion-icon> {{ 'admin.privacy.voteNote' | translate }}</p>
        </section>

        <div class="actions">
          <section class="card" aria-labelledby="export-title">
            <h3 id="export-title">{{ 'admin.privacy.exportTitle' | translate }}</h3>
            <p>{{ 'admin.privacy.exportText' | translate }}</p>
            <ion-button (click)="exportData()" [disabled]="busy()">
              <ion-icon slot="start" name="download-outline" aria-hidden="true"></ion-icon>
              {{ 'admin.privacy.export' | translate }}</ion-button>
            @if (exportMessage(); as m) { <p class="ok" role="status">{{ m }}</p> }
            @if (exportError(); as e) { <p class="server-error" role="alert">{{ e }}</p> }
          </section>

          <section class="card danger" aria-labelledby="erase-title">
            <h3 id="erase-title">{{ 'admin.privacy.eraseTitle' | translate }}</h3>
            @if (!s.erasureAllowed) {
              <p class="warning" role="note"><ion-icon name="alert-circle-outline" aria-hidden="true"></ion-icon>
                <span>{{ 'admin.privacy.blocked' | translate: { processes: blockingTitles() } }}</span></p>
            } @else {
              <div class="warning" role="note"><ion-icon name="information-circle-outline" aria-hidden="true"></ion-icon>
                <div>
                  @if (s.toAnonymize > 0) { <p>{{ 'admin.privacy.willAnonymize' | translate: { count: s.toAnonymize } }}</p> }
                  @if (s.toDelete > 0) { <p>{{ 'admin.privacy.willDelete' | translate: { count: s.toDelete } }}</p> }
                </div></div>
              <label class="confirm"><input type="checkbox" [checked]="confirmed()" (change)="confirmed.set($any($event.target).checked)" />
                {{ 'admin.privacy.confirm' | translate }}</label>
              <ion-button color="danger" (click)="erase()" [disabled]="busy() || !confirmed()">
                <ion-icon slot="start" name="trash-outline" aria-hidden="true"></ion-icon>
                {{ 'admin.privacy.erase' | translate }}</ion-button>
            }
            @if (eraseError(); as e) { <p class="server-error" role="alert">{{ e }}</p> }
          </section>
        </div>
      }
    </app-admin-page>
  `,
  styles: `
    .intro { margin: 0 0 16px; color: var(--ion-color-medium); max-width: 680px; }
    .find { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-end; }
    .find label { display: flex; flex-direction: column; gap: 4px; font-weight: 600; font-size: 0.875rem; flex: 1 1 260px; min-width: 0; }
    .find input { min-height: 48px; padding: 0 12px; font: inherit; font-weight: 400; font-size: 1rem; color: var(--ion-text-color, #1b2733);
      background: #fff; border: 1px solid #9aa8b8; border-radius: var(--vv-radius-control, 8px); width: 100%; box-sizing: border-box; }
    .find input:focus-visible, .confirm input:focus-visible { outline: 3px solid var(--ion-color-primary); outline-offset: 2px; }
    .find ion-button { margin: 0; min-height: 48px; }
    .help { color: var(--ion-color-medium); font-size: 0.875rem; }
    .help ion-icon { vertical-align: -2px; }
    .server-error { color: var(--ion-color-danger); font-weight: 600; }
    .ok { color: var(--ion-color-success); font-weight: 600; }
    .facts { display: grid; gap: 4px; margin: 0 0 12px; }
    .facts div { display: flex; justify-content: space-between; gap: 12px; border-bottom: 1px solid var(--vv-border-color); padding: 6px 0; }
    .facts dt { color: #4a5868; }
    .facts dd { margin: 0; font-weight: 700; text-align: right; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; font-size: 0.9375rem; }
    th { text-align: left; font-size: 0.8125rem; text-transform: uppercase; letter-spacing: 0.02em; color: #4a5868; background: #f2f4f7; padding: 10px 12px; }
    td { padding: 10px 12px; border-top: 1px solid var(--vv-border-color); vertical-align: top; overflow-wrap: anywhere; }
    td small { color: var(--ion-color-medium); }
    .process { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; margin-bottom: 4px; }
    .actions { display: grid; gap: 16px; grid-template-columns: 1fr; }
    @media (min-width: 900px) { .actions { grid-template-columns: 1fr 1fr; align-items: start; } }
    .actions .card { margin: 0; }
    .actions ion-button { margin: 8px 0 0; }
    .danger { border-color: #e7b7b4; }
    .danger h3 { color: var(--ion-color-danger); }
    .warning { display: flex; gap: 8px; align-items: flex-start; background: #fff7e0; border: 1px solid #f0d58a; color: #6b4e00;
      border-radius: var(--vv-radius-control, 8px); padding: 12px 14px; margin: 0 0 12px; }
    .warning p { margin: 0 0 4px; }
    .warning ion-icon { flex: none; margin-top: 3px; }
    .confirm { display: flex; gap: 10px; align-items: flex-start; min-height: 44px; cursor: pointer; }
    .confirm input { width: 20px; height: 20px; margin: 2px 0 0; flex: none; }
    @media (max-width: 767px) {
      thead { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
      table, tbody, tr, td { display: block; }
      tr { border-top: 1px solid var(--vv-border-color); padding: 10px 0; }
      td { border: 0; padding: 2px 0; display: flex; gap: 8px; flex-wrap: wrap; }
      td[data-label]::before { content: attr(data-label); flex: 0 0 35%; color: var(--ion-color-medium); font-size: 0.875rem; }
    }
  `,
})
export class PrivacyPage {
  private readonly api = inject(PrivacyApiService);
  private readonly translate = inject(TranslateService);
  protected readonly language = inject(LanguageService).current;
  protected readonly query = signal('');
  /** The query of the person shown, so actions always apply to what is on screen. */
  private readonly shownQuery = signal('');
  protected readonly subject = signal<PrivacySubject | null>(null);
  protected readonly busy = signal(false);
  protected readonly confirmed = signal(false);
  protected readonly findError = signal<string | null>(null);
  protected readonly exportMessage = signal<string | null>(null);
  protected readonly exportError = signal<string | null>(null);
  protected readonly eraseError = signal<string | null>(null);
  protected readonly done = signal<string | null>(null);

  protected readonly processCount = computed(() => new Set((this.subject()?.entries ?? [])
    .flatMap((entry) => entry.processes.map((process) => process.id))).size);
  protected readonly blockingTitles = computed(() => (this.subject()?.blockingProcesses ?? [])
    .map((process) => `«${process.title[this.language()] ?? process.title['es'] ?? process.id}»`).join(', '));

  protected async find(): Promise<void> {
    const query = this.query().trim();
    if (!query) return;
    this.reset();
    this.busy.set(true);
    try {
      this.subject.set(await this.api.search(query));
      this.shownQuery.set(query);
    } catch (e) {
      this.findError.set(this.message(e));
    } finally {
      this.busy.set(false);
    }
  }

  protected async exportData(): Promise<void> {
    this.busy.set(true);
    this.exportMessage.set(null);
    this.exportError.set(null);
    try {
      const blob = await this.api.export(this.shownQuery());
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${this.translate.instant('admin.privacy.fileName')}.json`;
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      this.exportMessage.set(this.translate.instant('admin.privacy.exported'));
    } catch (e) {
      this.exportError.set(this.message(e));
    } finally {
      this.busy.set(false);
    }
  }

  protected async erase(): Promise<void> {
    if (!this.confirmed()) return;
    this.busy.set(true);
    this.eraseError.set(null);
    try {
      const { deleted, anonymized } = await this.api.erase(this.shownQuery());
      this.reset();
      this.query.set('');
      this.done.set(this.translate.instant('admin.privacy.erased', { deleted, anonymized }));
    } catch (e) {
      this.eraseError.set(this.message(e));
    } finally {
      this.busy.set(false);
    }
  }

  private reset(): void {
    this.subject.set(null);
    this.shownQuery.set('');
    this.confirmed.set(false);
    this.findError.set(null);
    this.exportMessage.set(null);
    this.exportError.set(null);
    this.eraseError.set(null);
    this.done.set(null);
  }

  private message(e: unknown): string {
    if (e instanceof ApiError && e.detail && e.status !== 401) return e.detail;
    return this.translate.instant('common.loadError') as string;
  }
}
