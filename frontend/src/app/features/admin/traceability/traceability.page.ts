import { Component, computed, inject, signal } from '@angular/core';
import { IonButton, IonSpinner, IonText } from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  ADMIN_ACTIONS, Actor, AdminAction, AdminActionFilter, AdminActionPage, AdminActionsApiService,
} from '../../../core/admin/admin-actions-api.service';
import { ProcessApiService, ProcessSummary } from '../../../core/admin/process-api.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { LocalizedPipe } from '../../../shared/pipes/localized.pipe';
import { AdminPageComponent } from '../admin-page.component';

const PAGE_SIZE = 50;

/**
 * RF-19 (D-44): traceability of the administrative actions of the organization, for ADMIN and AUDITOR. Who, what, on
 * which object and when, newest first, with filters by dates, person, type of action and process. Read only: nothing
 * here can be edited or deleted (RNF-13).
 */
@Component({
  selector: 'app-traceability',
  imports: [AdminPageComponent, IonButton, IonSpinner, IonText, TranslatePipe, LocalDatePipe, LocalizedPipe],
  template: `
    <app-admin-page titleKey="admin.trace.title">
      <p class="intro">{{ 'admin.trace.intro' | translate }}</p>
      <form class="filters" (submit)="$event.preventDefault(); apply()" [attr.aria-label]="'admin.trace.filters' | translate">
        <label><span>{{ 'admin.trace.from' | translate }}</span>
          <input type="date" [value]="from()" (change)="from.set($any($event.target).value)" /></label>
        <label><span>{{ 'admin.trace.to' | translate }}</span>
          <input type="date" [value]="to()" (change)="to.set($any($event.target).value)" /></label>
        <label><span>{{ 'admin.trace.actor' | translate }}</span>
          <select (change)="actorId.set($any($event.target).value)">
            <option value="" [selected]="!actorId()">{{ 'admin.trace.all' | translate }}</option>
            @for (a of actors(); track a.actorId) {
              <option [value]="a.actorId" [selected]="a.actorId === actorId()">{{ a.actorName }}</option>
            }
          </select></label>
        <label><span>{{ 'admin.trace.action' | translate }}</span>
          <select (change)="action.set($any($event.target).value)">
            <option value="" [selected]="!action()">{{ 'admin.trace.all' | translate }}</option>
            @for (code of actionCodes; track code) {
              <option [value]="code" [selected]="code === action()">{{ 'admin.trace.actions.' + code | translate }}</option>
            }
          </select></label>
        <label><span>{{ 'admin.trace.process' | translate }}</span>
          <select (change)="processId.set($any($event.target).value)">
            <option value="" [selected]="!processId()">{{ 'admin.trace.all' | translate }}</option>
            @for (p of processes(); track p.id) {
              <option [value]="p.id" [selected]="'' + p.id === processId()">{{ p.title | localized: language() }}</option>
            }
          </select></label>
        <div class="buttons">
          <ion-button type="submit" color="primary">{{ 'admin.trace.apply' | translate }}</ion-button>
          <ion-button fill="clear" (click)="clear()">{{ 'admin.trace.clear' | translate }}</ion-button>
        </div>
      </form>
      @if (dateError()) { <p class="server-error" role="alert">{{ 'admin.trace.dateError' | translate }}</p> }

      @if (loading()) {
        <ion-spinner role="status" [attr.aria-label]="'common.loading' | translate" />
      } @else if (error()) {
        <ion-text color="danger"><p role="alert">{{ 'common.loadError' | translate }}</p></ion-text>
      } @else if (result(); as page) {
        <section class="card">
          <h3 id="trace-title">{{ 'admin.trace.listTitle' | translate }}</h3>
          @if (page.totalItems === 0) {
            <p class="help" role="status">{{ 'admin.trace.none' | translate }}</p>
          } @else {
            <div class="table-wrap">
              <table aria-labelledby="trace-title">
                <thead><tr>
                  <th scope="col">{{ 'admin.trace.col.when' | translate }}</th>
                  <th scope="col">{{ 'admin.trace.col.who' | translate }}</th>
                  <th scope="col">{{ 'admin.trace.col.what' | translate }}</th>
                  <th scope="col">{{ 'admin.trace.col.object' | translate }}</th>
                  <th scope="col">{{ 'admin.trace.col.process' | translate }}</th>
                </tr></thead>
                <tbody>
                  @for (a of page.items; track $index) {
                    <tr>
                      <td [attr.data-label]="'admin.trace.col.when' | translate">{{ a.occurredAt | localDate: language() }}</td>
                      <td [attr.data-label]="'admin.trace.col.who' | translate">{{ a.actorName }}</td>
                      <td [attr.data-label]="'admin.trace.col.what' | translate"><strong>{{ 'admin.trace.actions.' + a.action | translate }}</strong>
                        @if (detail(a); as d) { <span class="detail">&nbsp;·&nbsp;{{ d }}</span> }</td>
                      <td [attr.data-label]="'admin.trace.col.object' | translate">{{ object(a) }}</td>
                      <td [attr.data-label]="'admin.trace.col.process' | translate">{{ processName(a.processId) }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            <div class="pager">
              <p class="help" role="status">{{ 'admin.trace.showing' | translate: { from: first(), to: last(), total: page.totalItems } }}</p>
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
    .intro { margin: 0 0 16px; color: var(--ion-color-medium); max-width: 640px; }
    .filters { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-end; background: #fff; border: 1px solid var(--vv-border-color);
      border-radius: var(--vv-radius-card); padding: 16px; margin-bottom: 16px; }
    .filters label { display: flex; flex-direction: column; gap: 4px; font-weight: 600; font-size: 0.875rem; flex: 1 1 170px; min-width: 0; }
    .filters input, .filters select { min-height: 48px; padding: 0 12px; font: inherit; font-weight: 400; font-size: 1rem; color: var(--ion-text-color, #1b2733);
      background: #fff; border: 1px solid #9aa8b8; border-radius: var(--vv-radius-control, 8px); width: 100%; box-sizing: border-box; }
    .filters input:focus-visible, .filters select:focus-visible { outline: 3px solid var(--ion-color-primary); outline-offset: 2px; }
    .buttons { display: flex; gap: 4px; align-items: center; }
    .help { color: var(--ion-color-medium); font-size: 0.875rem; }
    .detail { color: #4a5868; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; font-size: 0.9375rem; }
    th { text-align: left; font-size: 0.8125rem; text-transform: uppercase; letter-spacing: 0.02em; color: #4a5868; background: #f2f4f7; padding: 10px 12px; }
    td { padding: 10px 12px; border-top: 1px solid var(--vv-border-color); vertical-align: top; }
    .pager { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; justify-content: space-between; margin-top: 12px; }
    .pager ion-button { margin: 0 0 0 6px; }
    @media (max-width: 767px) {
      thead { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
      table, tbody, tr, td { display: block; }
      tr { border-top: 1px solid var(--vv-border-color); padding: 10px 0; }
      td { border: 0; padding: 2px 0; display: flex; gap: 8px; }
      td[data-label]::before { content: attr(data-label); flex: 0 0 35%; color: var(--ion-color-medium); font-size: 0.875rem; }
    }
  `,
})
export class TraceabilityPage {
  private readonly api = inject(AdminActionsApiService);
  private readonly processApi = inject(ProcessApiService);
  private readonly translate = inject(TranslateService);
  protected readonly language = inject(LanguageService).current;
  protected readonly actionCodes = ADMIN_ACTIONS;

  protected readonly from = signal('');
  protected readonly to = signal('');
  protected readonly actorId = signal('');
  protected readonly action = signal('');
  protected readonly processId = signal('');
  protected readonly actors = signal<Actor[]>([]);
  protected readonly processes = signal<ProcessSummary[]>([]);
  protected readonly result = signal<AdminActionPage | null>(null);
  protected readonly pageIndex = signal(0);
  protected readonly loading = signal(false);
  protected readonly error = signal(false);
  protected readonly dateError = signal(false);
  private applied: AdminActionFilter = {};
  protected readonly first = computed(() => (this.result()?.totalItems ? this.pageIndex() * PAGE_SIZE + 1 : 0));
  protected readonly last = computed(() => Math.min((this.pageIndex() + 1) * PAGE_SIZE, this.result()?.totalItems ?? 0));

  async ionViewWillEnter(): Promise<void> {
    void this.api.actors().then((a) => this.actors.set(a), () => this.actors.set([]));
    void this.processApi.list().then((p) => this.processes.set(p), () => this.processes.set([]));
    await this.load();
  }

  protected async apply(): Promise<void> {
    // Dates of the form are days in the user's time zone; «to» includes the whole day.
    const from = this.from() ? new Date(`${this.from()}T00:00:00`) : null;
    const to = this.to() ? new Date(`${this.to()}T00:00:00`) : null;
    if (to) to.setDate(to.getDate() + 1);
    if (from && to && from >= to) {
      this.dateError.set(true);
      return;
    }
    this.dateError.set(false);
    this.applied = {
      from: from?.toISOString(), to: to?.toISOString(), actorId: this.actorId() || undefined,
      action: this.action() || undefined, processId: this.processId() ? Number(this.processId()) : undefined,
    };
    this.pageIndex.set(0);
    await this.load();
  }

  protected async clear(): Promise<void> {
    this.from.set(''); this.to.set(''); this.actorId.set(''); this.action.set(''); this.processId.set('');
    this.dateError.set(false);
    this.applied = {};
    this.pageIndex.set(0);
    await this.load();
  }

  protected async go(delta: number): Promise<void> {
    this.pageIndex.update((page) => Math.max(0, page + delta));
    await this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      this.result.set(await this.api.search(this.applied, this.pageIndex(), PAGE_SIZE));
      this.error.set(false);
    } catch {
      this.error.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  /** «Sobre qué»: the object with its name; voters only by number (no personal data in the traceability). */
  protected object(a: AdminAction): string {
    const type = this.translate.instant(`admin.trace.objects.${a.objectType}`) as string;
    if (a.objectType === 'VOTER') {
      return `${type} n.º ${a.objectId ?? '?'}${a.objectLabel ? ` · ${a.objectLabel}` : ''}`;
    }
    if (a.objectLabel) return `${type} «${a.objectLabel}»`;
    if (a.objectType === 'PROCESS' && a.objectId) return `${type} «${this.processName(Number(a.objectId))}»`;
    return a.objectId ? `${type} ${a.objectId}` : type;
  }

  protected detail(a: AdminAction): string | null {
    if (!a.detail) return null;
    if (a.action === 'PERSONAL_DATA_ERASED' && /^\d+:\d+$/.test(a.detail)) {
      const [deleted, anonymized] = a.detail.split(':');
      return this.translate.instant('admin.trace.details.erased', { deleted, anonymized }) as string;
    }
    if (a.action === 'REMINDER_SCHEDULED' || a.action === 'REMINDER_CANCELLED') {
      const when = new LocalDatePipe().transform(a.detail, this.language());
      return this.translate.instant('admin.trace.details.reminder', { value: when }) as string;
    }
    for (const key of [`admin.trace.details.${a.action}.${a.detail}`, `admin.trace.details.${a.action}.value`]) {
      const text = this.translate.instant(key, { value: a.detail }) as string;
      if (text !== key) return text;
    }
    return a.detail;
  }

  protected processName(id: number | null): string {
    if (!id) return '—';
    const process = this.processes().find((p) => p.id === id);
    if (!process) return `n.º ${id}`;
    const title = process.title as Record<string, string | undefined>;
    return title[this.language()] ?? title['es'] ?? title['en'] ?? `n.º ${id}`;
  }
}
