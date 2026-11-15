import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { IonButton, IonIcon, IonSpinner } from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ApiError } from '../../../core/admin/api-error';
import {
  NotificationSummary, NotificationType, NotificationsApiService, Reminder,
} from '../../../core/admin/notifications-api.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';

/**
 * A5, RF-17 (D-45): reminders to the people who have not voted yet and the emails of the process. Reminders show
 * how many people they went to, never who; failed reminders are only counted for the same reason.
 */
@Component({
  selector: 'app-notifications-panel',
  imports: [IonButton, IonIcon, IonSpinner, TranslatePipe, LocalDatePipe],
  template: `
    @if (data(); as d) {
      <section class="card" aria-labelledby="reminders-title">
        <h3 id="reminders-title">{{ 'admin.notifications.remindersTitle' | translate }}</h3>
        <p class="help">{{ 'admin.notifications.remindersHelp' | translate }}</p>
        @if (open()) {
          @if (confirming()) {
            <div class="confirm" role="group" aria-labelledby="confirm-text">
              <p id="confirm-text">{{ 'admin.notifications.confirmNow' | translate: { count: pending() } }}</p>
              <ion-button (click)="sendNow()" [disabled]="busy()">{{ 'admin.notifications.confirmSend' | translate }}</ion-button>
              <ion-button fill="outline" (click)="confirming.set(false)">{{ 'admin.notifications.keep' | translate }}</ion-button>
            </div>
          } @else {
            <div class="actions">
              <ion-button (click)="confirming.set(true)" [disabled]="busy() || full() || pending() === 0">
                <ion-icon slot="start" name="send-outline" aria-hidden="true"></ion-icon>
                {{ 'admin.notifications.sendNow' | translate }}</ion-button>
              <form class="schedule" novalidate (submit)="$event.preventDefault(); schedule()">
                <label>{{ 'admin.notifications.scheduleAt' | translate }}
                  <input type="datetime-local" [min]="minLocal()" [max]="maxLocal()" [value]="when()"
                         (input)="when.set($any($event.target).value)" [attr.aria-describedby]="formError() ? 'reminder-error' : null" /></label>
                <ion-button type="submit" fill="outline" [disabled]="busy() || full() || !when()">
                  <ion-icon slot="start" name="time-outline" aria-hidden="true"></ion-icon>
                  {{ 'admin.notifications.schedule' | translate }}</ion-button>
              </form>
            </div>
          }
          <p class="help">{{ 'admin.notifications.limit' | translate: { used: used(), max: d.maxReminders } }}
            @if (pending() === 0) { {{ 'admin.notifications.nobodyLeft' | translate }} }</p>
        } @else {
          <p class="help">{{ 'admin.notifications.onlyOpen' | translate }}</p>
        }
        @if (formError(); as e) { <p id="reminder-error" class="server-error" role="alert">{{ e }}</p> }
        @if (message(); as m) { <p class="ok" role="status">{{ m }}</p> }
        @if (d.reminders.length > 0) {
          <!-- DEF-07 (WCAG 2.1.1): scrollable tables are focusable regions with a name -->
          <div class="table-wrap" tabindex="0" role="region" [attr.aria-label]="'admin.notifications.remindersTitle' | translate">
            <table>
              <caption class="sr-only">{{ 'admin.notifications.remindersTitle' | translate }}</caption>
              <thead><tr><th scope="col">{{ 'admin.notifications.col.when' | translate }}</th>
                <th scope="col">{{ 'admin.notifications.col.status' | translate }}</th>
                <th scope="col">{{ 'admin.notifications.col.recipients' | translate }}</th><th scope="col"><span class="sr-only">{{ 'admin.notifications.col.actions' | translate }}</span></th></tr></thead>
              <tbody>
                @for (r of d.reminders; track r.id) {
                  <tr>
                    <td [attr.data-label]="'admin.notifications.col.when' | translate">{{ r.sendAt | localDate: language() }}</td>
                    <td [attr.data-label]="'admin.notifications.col.status' | translate"><span class="pill" [class]="r.status">{{ 'admin.notifications.reminderStatus.' + r.status | translate }}</span></td>
                    <td [attr.data-label]="'admin.notifications.col.recipients' | translate">{{ r.recipients === null ? '—' : ('admin.notifications.people' | translate: { count: r.recipients }) }}</td>
                    <td>@if (r.status === 'SCHEDULED') {
                      <ion-button fill="clear" size="small" color="danger" (click)="cancel(r)" [disabled]="busy()"
                                  [attr.aria-label]="'admin.notifications.cancelLabel' | translate: { date: (r.sendAt | localDate: language()) }">
                        {{ 'admin.notifications.cancel' | translate }}</ion-button> }</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>

      <section class="card" aria-labelledby="emails-title">
        <h3 id="emails-title">{{ 'admin.notifications.emailsTitle' | translate }}</h3>
        <div class="table-wrap" tabindex="0" role="region" aria-labelledby="emails-title">
          <table class="counts">
            <caption class="sr-only">{{ 'admin.notifications.emailsTitle' | translate }}</caption>
            <thead><tr><th scope="col">{{ 'admin.notifications.col.type' | translate }}</th>
              <th scope="col">{{ 'admin.notifications.col.delivered' | translate }}</th><th scope="col">{{ 'admin.notifications.col.pending' | translate }}</th>
              <th scope="col">{{ 'admin.notifications.col.failed' | translate }}</th><th scope="col">{{ 'admin.notifications.col.notNeeded' | translate }}</th></tr></thead>
            <tbody>
              @for (t of types; track t) {
                <tr>
                  <th scope="row">{{ 'admin.notifications.type.' + t | translate }}</th>
                  <td [attr.data-label]="'admin.notifications.col.delivered' | translate">{{ d.counts[t].delivered }}</td>
                  <td [attr.data-label]="'admin.notifications.col.pending' | translate">{{ d.counts[t].pending }}</td>
                  <td [attr.data-label]="'admin.notifications.col.failed' | translate" [class.bad]="d.counts[t].failed > 0">{{ d.counts[t].failed }}</td>
                  <td [attr.data-label]="'admin.notifications.col.notNeeded' | translate">{{ d.counts[t].cancelled }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <p class="help">{{ 'admin.notifications.notNeededHelp' | translate }}</p>

        @if (totalFailed() > 0) {
          <h4>{{ 'admin.notifications.failedTitle' | translate }}</h4>
          @if (d.failed.length > 0) {
            <div class="table-wrap" tabindex="0" role="region" [attr.aria-label]="'admin.notifications.failedTitle' | translate">
              <table>
                <caption class="sr-only">{{ 'admin.notifications.failedTitle' | translate }}</caption>
                <thead><tr><th scope="col">{{ 'admin.notifications.col.person' | translate }}</th>
                  <th scope="col">{{ 'admin.notifications.col.type' | translate }}</th>
                  <th scope="col">{{ 'admin.notifications.col.attempts' | translate }}</th>
                  <th scope="col">{{ 'admin.notifications.col.reason' | translate }}</th></tr></thead>
                <tbody>
                  @for (f of d.failed; track $index) {
                    <tr>
                      <td [attr.data-label]="'admin.notifications.col.person' | translate">{{ f.displayName }}<br /><small>{{ f.email }}</small></td>
                      <td [attr.data-label]="'admin.notifications.col.type' | translate">{{ 'admin.notifications.type.' + f.type | translate }}</td>
                      <td [attr.data-label]="'admin.notifications.col.attempts' | translate">{{ f.attempts }}</td>
                      <td [attr.data-label]="'admin.notifications.col.reason' | translate">{{ f.reason || '—' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
          @if (d.counts.REMINDER.failed > 0) {
            <p class="help">{{ 'admin.notifications.failedReminders' | translate: { count: d.counts.REMINDER.failed } }}</p>
          }
          <ion-button fill="outline" (click)="retry()" [disabled]="busy()">
            <ion-icon slot="start" name="refresh-outline" aria-hidden="true"></ion-icon>
            {{ 'admin.notifications.retry' | translate: { count: totalFailed() } }}</ion-button>
        }
        @if (retryMessage(); as m) { <p class="ok" role="status">{{ m }}</p> }
        @if (retryError(); as e) { <p class="server-error" role="alert">{{ e }}</p> }
      </section>
    } @else if (error()) {
      <p class="server-error" role="alert">{{ 'admin.notifications.loadError' | translate }}</p>
    } @else {
      <ion-spinner role="status" [attr.aria-label]="'common.loading' | translate" />
    }
  `,
  styles: `
    h4 { margin: 20px 0 8px; font-size: 1rem; }
    .help { color: var(--ion-color-medium); font-size: 0.875rem; }
    .actions { display: flex; flex-wrap: wrap; gap: 16px; align-items: flex-end; }
    .actions ion-button { margin: 0; }
    .schedule { display: flex; flex-wrap: wrap; gap: 8px; align-items: flex-end; }
    .schedule label { display: flex; flex-direction: column; gap: 4px; font-weight: 600; font-size: 0.875rem; }
    .schedule input { min-height: 44px; padding: 0 12px; font: inherit; font-weight: 400; font-size: 1rem; color: var(--ion-text-color, #1b2733);
      background: #fff; border: 1px solid #9aa8b8; border-radius: var(--vv-radius-control, 8px); }
    .schedule input:focus-visible { outline: 3px solid var(--ion-color-primary); outline-offset: 2px; }
    .confirm { background: #f2f6fb; border: 1px solid var(--vv-border-color); border-radius: var(--vv-radius-card); padding: 12px 16px; }
    .confirm p { margin: 0 0 8px; font-weight: 600; }
    .confirm ion-button { margin: 0 8px 0 0; }
    .server-error { color: var(--ion-color-danger); font-weight: 600; }
    .ok { color: var(--ion-color-success); font-weight: 600; }
    .table-wrap { overflow-x: auto; margin-top: 12px; }
    table { width: 100%; border-collapse: collapse; font-size: 0.9375rem; }
    th { text-align: left; font-size: 0.8125rem; text-transform: uppercase; letter-spacing: 0.02em; color: #4a5868; background: #f2f4f7; padding: 10px 12px; }
    tbody th { text-transform: none; font-size: 0.9375rem; letter-spacing: 0; color: inherit; background: none; border-top: 1px solid var(--vv-border-color); }
    td { padding: 10px 12px; border-top: 1px solid var(--vv-border-color); vertical-align: top; }
    td small { color: var(--ion-color-medium); }
    td.bad { color: var(--ion-color-danger); font-weight: 700; }
    .pill { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 0.8125rem; font-weight: 600; background: #e3e8ef; color: #34404d; }
    .pill.SCHEDULED { background: #e5effa; color: #0b4f8a; }
    .pill.SENT { background: #e4f4ea; color: #1d6b3a; }
    @media (max-width: 767px) {
      thead { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
      table, tbody, tr, td, tbody th { display: block; }
      tr { border-top: 1px solid var(--vv-border-color); padding: 10px 0; }
      td, tbody th { border: 0; padding: 2px 0; display: flex; gap: 8px; }
      td[data-label]::before { content: attr(data-label); flex: 0 0 45%; color: var(--ion-color-medium); font-size: 0.875rem; }
    }
  `,
})
export class NotificationsPanelComponent {
  readonly processId = input.required<number>();
  /** People who have not voted yet (A5), shown before sending a reminder now. */
  readonly pending = input<number>(0);
  /** Changes whenever the page reloads the participation, so both stay in step. */
  readonly version = input<number>(0);

  private readonly api = inject(NotificationsApiService);
  private readonly translate = inject(TranslateService);
  protected readonly language = inject(LanguageService).current;
  protected readonly types: readonly NotificationType[] = ['INVITATION', 'REMINDER', 'CLOSING'];
  protected readonly data = signal<NotificationSummary | null>(null);
  protected readonly error = signal(false);
  protected readonly busy = signal(false);
  protected readonly confirming = signal(false);
  protected readonly when = signal('');
  protected readonly formError = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly retryMessage = signal<string | null>(null);
  protected readonly retryError = signal<string | null>(null);

  protected readonly open = computed(() => this.data()?.status === 'OPEN');
  protected readonly used = computed(() => (this.data()?.reminders ?? []).filter((r) => r.status !== 'CANCELLED').length);
  protected readonly full = computed(() => this.used() >= (this.data()?.maxReminders ?? 0));
  protected readonly totalFailed = computed(() => {
    const counts = this.data()?.counts;
    return counts ? counts.INVITATION.failed + counts.REMINDER.failed + counts.CLOSING.failed : 0;
  });
  protected readonly minLocal = computed(() => toLocalInput(new Date().toISOString()));
  protected readonly maxLocal = computed(() => (this.data() ? toLocalInput(this.data()!.closesAt) : ''));

  constructor() {
    effect(() => {
      const id = this.processId();
      this.version();
      void this.load(id);
    });
  }

  private async load(id: number): Promise<void> {
    try {
      const summary = await this.api.summary(id);
      if (id === this.processId()) {
        this.data.set(summary);
        this.error.set(false);
      }
    } catch {
      this.error.set(true);
    }
  }

  protected async sendNow(): Promise<void> {
    await this.run(() => this.api.scheduleReminder(this.processId()), 'admin.notifications.sentNow');
    this.confirming.set(false);
  }

  protected async schedule(): Promise<void> {
    const value = this.when();
    if (!value) return;
    const date = new Date(value);
    const closes = new Date(this.data()!.closesAt);
    if (Number.isNaN(date.getTime()) || date.getTime() < Date.now() - 60_000 || date >= closes) {
      this.message.set(null);
      this.formError.set(this.translate.instant('admin.notifications.outOfPeriod') as string);
      return;
    }
    const ok = await this.run(() => this.api.scheduleReminder(this.processId(), date.toISOString()),
      'admin.notifications.scheduled');
    if (ok) this.when.set('');
  }

  protected async cancel(reminder: Reminder): Promise<void> {
    await this.run(() => this.api.cancelReminder(this.processId(), reminder.id), 'admin.notifications.cancelled');
  }

  private async run(action: () => Promise<unknown>, okKey: string): Promise<boolean> {
    this.busy.set(true);
    this.formError.set(null);
    this.message.set(null);
    let ok = false;
    try {
      await action();
      ok = true;
    } catch (e) {
      this.formError.set(e instanceof ApiError && e.detail ? e.detail : (this.translate.instant('common.loadError') as string));
    }
    await this.load(this.processId());
    if (ok) this.message.set(this.translate.instant(okKey) as string);
    this.busy.set(false);
    return ok;
  }

  protected async retry(): Promise<void> {
    this.busy.set(true);
    this.retryMessage.set(null);
    this.retryError.set(null);
    let retried: number | null = null;
    try {
      retried = (await this.api.retryFailed(this.processId())).retried;
    } catch (e) {
      this.retryError.set(e instanceof ApiError && e.detail ? e.detail : (this.translate.instant('common.loadError') as string));
    }
    await this.load(this.processId());
    if (retried !== null) {
      this.retryMessage.set(this.translate.instant('admin.notifications.retried', { count: retried }) as string);
    }
    this.busy.set(false);
  }
}

/** `datetime-local` works with the browser local time; the API works with instants. */
function toLocalInput(iso: string): string {
  const date = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
