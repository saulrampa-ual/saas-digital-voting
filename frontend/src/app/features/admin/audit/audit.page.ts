import { Component, computed, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { IonButton, IonIcon, IonSpinner, IonText } from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { LanguageService } from '../../../core/i18n/language.service';
import { ProcessStatus } from '../../../core/admin/process-api.service';
import { AuditEventPage, ChainCheck, ResultsApiService } from '../../../core/admin/results-api.service';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { AdminPageComponent } from '../admin-page.component';
import { ProcessPickerComponent } from '../process-picker/process-picker.component';

const PAGE_SIZE = 50;

/**
 * H1 (RF-12): the auditor reads the hash chain of the ballot box and verifies it. Read only. Vote events show neither
 * identity nor time (D-18). The traceability of administrative actions (RF-19) arrives in P6.
 */
@Component({
  selector: 'app-audit',
  imports: [AdminPageComponent, ProcessPickerComponent, IonButton, IonIcon, IonSpinner, IonText, TranslatePipe, LocalDatePipe],
  template: `
    <app-admin-page titleKey="admin.audit.title">
      <div class="top">
        <p class="intro">{{ 'admin.audit.intro' | translate }}</p>
        <app-process-picker [statuses]="statuses" [selected]="selectedId()" (selectedChange)="select($event)"
                            emptyKey="admin.audit.empty" />
      </div>
      @if (loading()) {
        <ion-spinner role="status" [attr.aria-label]="'common.loading' | translate" />
      } @else if (error()) {
        <ion-text color="danger"><p role="alert">{{ 'common.loadError' | translate }}</p></ion-text>
      } @else if (events(); as page) {
        <section class="integrity" [class.ok]="check()?.intact === true" [class.bad]="check()?.intact === false" aria-live="polite">
          <div class="integrity-text">
            @if (!check()) {
              <h3>{{ 'admin.audit.neverTitle' | translate }}</h3>
              <p>{{ 'admin.audit.neverText' | translate }}</p>
            } @else if (check()!.intact) {
              <h3><ion-icon name="shield-checkmark-outline" aria-hidden="true"></ion-icon> {{ 'admin.audit.okTitle' | translate }}</h3>
              <p>{{ 'admin.audit.okText' | translate: { count: check()!.eventsChecked, date: (check()!.checkedAt | localDate: language()) } }}</p>
            } @else {
              <h3><ion-icon name="alert-circle-outline" aria-hidden="true"></ion-icon> {{ 'admin.audit.badTitle' | translate }}</h3>
              <p>{{ 'admin.audit.badText' | translate: { date: (check()!.checkedAt | localDate: language()) } }}</p>
              <ul class="problems">
                @for (problem of check()!.problems; track $index) {
                  <li>@if (problem.sequence) { <span class="mono">#{{ seq(problem.sequence) }}</span> · }
                    {{ 'admin.audit.problem.' + problem.code | translate }}</li>
                }
              </ul>
            }
            <p class="final">
              @if (page.finalHash) {
                {{ 'admin.audit.finalHash' | translate }} <span class="mono hash">{{ page.finalHash }}</span>
              } @else {
                {{ 'admin.audit.finalPending' | translate: { date: (page.closesAt | localDate: language()) } }}
              }
            </p>
          </div>
          <ion-button color="primary" (click)="verify()" [disabled]="verifying()">
            @if (verifying()) { <ion-spinner name="crescent" aria-hidden="true"></ion-spinner> }
            {{ 'admin.audit.verify' | translate }}</ion-button>
        </section>
        @if (verifyError()) { <p class="server-error" role="alert">{{ verifyError() }}</p> }

        <section class="card">
          <h3 id="chain-title">{{ 'admin.audit.chainTitle' | translate }}</h3>
          @if (page.totalItems === 0) {
            <p class="help">{{ 'admin.audit.noEvents' | translate }}</p>
          } @else {
            <div class="table-wrap">
              <table aria-labelledby="chain-title">
                <thead><tr>
                  <th scope="col">{{ 'admin.audit.col.seq' | translate }}</th>
                  <th scope="col">{{ 'admin.audit.col.event' | translate }}</th>
                  <th scope="col">{{ 'admin.audit.col.hash' | translate }}</th>
                  <th scope="col">{{ 'admin.audit.col.when' | translate }}</th>
                </tr></thead>
                <tbody>
                  @for (e of page.items; track e.sequence) {
                    <tr>
                      <td class="mono" [attr.data-label]="'admin.audit.col.seq' | translate">{{ seq(e.sequence) }}</td>
                      <td [attr.data-label]="'admin.audit.col.event' | translate"><strong>{{ 'admin.audit.event.' + e.type | translate }}</strong></td>
                      <td class="mono" [attr.data-label]="'admin.audit.col.hash' | translate" [title]="e.eventHash">
                        {{ short(e.eventHash) }}<span class="sr-only">{{ e.eventHash }}</span></td>
                      <td [attr.data-label]="'admin.audit.col.when' | translate">
                        @if (e.occurredAt) { {{ e.occurredAt | localDate: language() }} }
                        @else { <span class="no-time">{{ 'admin.audit.noTime' | translate }}</span> }</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            <div class="pager">
              <p class="help" role="status">{{ 'admin.audit.showing' | translate: { from: first(), to: last(), total: page.totalItems } }}</p>
              <div>
                <ion-button fill="outline" size="small" [disabled]="pageIndex() === 0" (click)="go(-1)">
                  {{ 'admin.census.previous' | translate }}</ion-button>
                <ion-button fill="outline" size="small" [disabled]="last() >= page.totalItems" (click)="go(1)">
                  {{ 'admin.census.next' | translate }}</ion-button>
              </div>
            </div>
          }
        </section>
      }
    </app-admin-page>
  `,
  styles: `
    .top { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
    .intro { margin: 0; color: var(--ion-color-medium); max-width: 520px; }
    .integrity { display: flex; flex-wrap: wrap; gap: 16px; align-items: center; justify-content: space-between; background: #fff;
      border: 2px solid #c9d2dd; border-radius: var(--vv-radius-card); padding: 16px 20px; margin-bottom: 16px; }
    .integrity.ok { border-color: var(--ion-color-success); }
    .integrity.bad { border-color: var(--ion-color-danger); }
    .integrity-text { flex: 1 1 380px; }
    .integrity h3 { display: flex; gap: 8px; align-items: center; margin: 0 0 4px; font-size: 1.125rem; }
    .ok h3 { color: var(--ion-color-success); }
    .bad h3 { color: var(--ion-color-danger); }
    .integrity p { margin: 0 0 4px; }
    .final { font-size: 0.875rem; color: #4a5868; }
    .hash { overflow-wrap: anywhere; font-size: 0.8125rem; }
    .problems { margin: 4px 0 8px; padding-left: 1.25rem; color: var(--ion-color-danger); }
    .help { color: var(--ion-color-medium); font-size: 0.875rem; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; font-size: 0.9375rem; }
    th { text-align: left; font-size: 0.8125rem; text-transform: uppercase; letter-spacing: 0.02em; color: #4a5868; background: #f2f4f7; padding: 10px 12px; }
    td { padding: 10px 12px; border-top: 1px solid var(--vv-border-color); }
    .no-time { color: var(--ion-color-medium); font-size: 0.875rem; }
    .pager { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; justify-content: space-between; margin-top: 12px; }
    .pager ion-button { margin: 0 0 0 6px; }
    @media (max-width: 767px) {
      thead { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
      table, tbody, tr, td { display: block; }
      tr { border-top: 1px solid var(--vv-border-color); padding: 10px 0; }
      td { border: 0; padding: 2px 0; display: flex; gap: 8px; }
      td[data-label]::before { content: attr(data-label); flex: 0 0 35%; color: var(--ion-color-medium); font-size: 0.875rem; font-family: var(--ion-font-family); }
    }
  `,
})
export class AuditPage {
  readonly proceso = input<string | undefined>();

  private readonly api = inject(ResultsApiService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  protected readonly language = inject(LanguageService).current;
  protected readonly statuses: readonly ProcessStatus[] = ['SCHEDULED', 'OPEN', 'CLOSED', 'TALLIED', 'ARCHIVED'];
  protected readonly selectedId = computed(() => (this.proceso() ? Number(this.proceso()) : null));
  protected readonly events = signal<AuditEventPage | null>(null);
  protected readonly check = signal<ChainCheck | null>(null);
  protected readonly pageIndex = signal(0);
  protected readonly loading = signal(false);
  protected readonly error = signal(false);
  protected readonly verifying = signal(false);
  protected readonly verifyError = signal<string | null>(null);
  protected readonly first = computed(() => (this.events()?.totalItems ? this.pageIndex() * PAGE_SIZE + 1 : 0));
  protected readonly last = computed(() => Math.min((this.pageIndex() + 1) * PAGE_SIZE, this.events()?.totalItems ?? 0));

  ionViewWillEnter(): void {
    if (this.selectedId()) void this.load(true);
  }

  protected async select(id: number): Promise<void> {
    this.pageIndex.set(0);
    await this.router.navigate([], { queryParams: { proceso: id }, replaceUrl: true });
    await this.load(true);
  }

  protected async go(delta: number): Promise<void> {
    this.pageIndex.update((page) => Math.max(0, page + delta));
    await this.load(false);
  }

  private async load(withCheck: boolean): Promise<void> {
    const id = this.selectedId();
    if (!id) return;
    this.loading.set(withCheck);
    try {
      const [events, check] = await Promise.all([
        this.api.auditEvents(id, this.pageIndex(), PAGE_SIZE),
        withCheck ? this.api.lastCheck(id) : Promise.resolve(this.check()),
      ]);
      this.events.set(events);
      this.check.set(check);
      this.error.set(false);
    } catch {
      this.error.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  protected async verify(): Promise<void> {
    const id = this.selectedId();
    if (!id) return;
    this.verifying.set(true);
    this.verifyError.set(null);
    try {
      this.check.set(await this.api.verifyChain(id));
      this.events.set(await this.api.auditEvents(id, this.pageIndex(), PAGE_SIZE));
    } catch {
      this.verifyError.set(this.translate.instant('common.loadError'));
    } finally {
      this.verifying.set(false);
    }
  }

  protected seq(value: number): string {
    return String(value).padStart(4, '0');
  }

  protected short(hash: string): string {
    return `${hash.slice(0, 4)}…${hash.slice(-4)}`;
  }
}
