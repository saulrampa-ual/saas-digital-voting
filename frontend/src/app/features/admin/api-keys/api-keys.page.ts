import { Component, computed, inject, signal } from '@angular/core';
import { IonButton, IonIcon, IonSpinner } from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ApiError } from '../../../core/admin/api-error';
import { ApiKey, ApiKeyList, ApiKeysApiService, CreatedApiKey } from '../../../core/admin/api-keys-api.service';
import { APP_CONFIG } from '../../../core/config/app-config';
import { LanguageService } from '../../../core/i18n/language.service';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { AdminPageComponent } from '../admin-page.component';

/**
 * RF-21 (D-47): the ADMIN creates and revokes the keys its integrations use to read processes, participation and
 * results through the API. No prototype (IMP-06): same style as the other console screens. The secret is shown once.
 */
@Component({
  selector: 'app-api-keys',
  imports: [AdminPageComponent, IonButton, IonIcon, IonSpinner, TranslatePipe, LocalDatePipe],
  template: `
    <app-admin-page titleKey="admin.apiKeys.title">
      <p class="intro">{{ 'admin.apiKeys.intro' | translate }}</p>

      @if (created(); as c) {
        <section class="card secret" aria-labelledby="secret-title" role="region">
          <h3 id="secret-title"><ion-icon name="key-outline" aria-hidden="true"></ion-icon> {{ 'admin.apiKeys.createdTitle' | translate: { name: c.key.name } }}</h3>
          <p class="warning" role="alert"><ion-icon name="alert-circle-outline" aria-hidden="true"></ion-icon>
            <span>{{ 'admin.apiKeys.onlyOnce' | translate }}</span></p>
          <dl class="creds">
            <div><dt>{{ 'admin.apiKeys.clientId' | translate }}</dt><dd><code>{{ c.key.clientId }}</code>
              <ion-button fill="clear" size="small" (click)="copy(c.key.clientId, 'id')" [attr.aria-label]="'admin.apiKeys.copyId' | translate">
                <ion-icon slot="icon-only" name="copy-outline" aria-hidden="true"></ion-icon></ion-button></dd></div>
            <div><dt>{{ 'admin.apiKeys.secret' | translate }}</dt><dd><code class="secret-value">{{ c.clientSecret }}</code>
              <ion-button fill="clear" size="small" (click)="copy(c.clientSecret, 'secret')" [attr.aria-label]="'admin.apiKeys.copySecret' | translate">
                <ion-icon slot="icon-only" name="copy-outline" aria-hidden="true"></ion-icon></ion-button></dd></div>
            <div><dt>{{ 'admin.apiKeys.tokenUrl' | translate }}</dt><dd><code>{{ c.tokenUrl }}</code></dd></div>
          </dl>
          @if (copied(); as what) { <p class="ok" role="status">{{ 'admin.apiKeys.copied.' + what | translate }}</p> }
          <p class="help">{{ 'admin.apiKeys.howTo' | translate }}</p>
          <pre class="example"><code>{{ example(c) }}</code></pre>
          <ion-button (click)="created.set(null)">{{ 'admin.apiKeys.saved' | translate }}</ion-button>
        </section>
      }

      <section class="card" aria-labelledby="new-title">
        <h3 id="new-title">{{ 'admin.apiKeys.newTitle' | translate }}</h3>
        <form class="new" novalidate (submit)="$event.preventDefault(); create()">
          <label>{{ 'admin.apiKeys.name' | translate }}
            <input type="text" maxlength="60" [value]="name()" (input)="name.set($any($event.target).value)"
                   [attr.aria-describedby]="'name-help'" /></label>
          <ion-button type="submit" [disabled]="busy() || !name().trim() || full()">
            <ion-icon slot="start" name="add-outline" aria-hidden="true"></ion-icon>
            {{ 'admin.apiKeys.create' | translate }}</ion-button>
        </form>
        <p id="name-help" class="help">{{ 'admin.apiKeys.limit' | translate: { used: activeCount(), max: list()?.maxActive ?? 5 } }}</p>
        @if (formError(); as e) { <p class="server-error" role="alert">{{ e }}</p> }
        <details>
          <summary>{{ 'admin.apiKeys.scopeTitle' | translate }}</summary>
          <ul class="scope">
            <li>{{ 'admin.apiKeys.scope.processes' | translate }}</li>
            <li>{{ 'admin.apiKeys.scope.participation' | translate }}</li>
            <li>{{ 'admin.apiKeys.scope.results' | translate }}</li>
          </ul>
          <p class="help">{{ 'admin.apiKeys.scopeNot' | translate }}</p>
        </details>
      </section>

      <section class="card" aria-labelledby="list-title">
        <h3 id="list-title">{{ 'admin.apiKeys.listTitle' | translate }}</h3>
        @if (loading()) {
          <ion-spinner role="status" [attr.aria-label]="'common.loading' | translate" />
        } @else if (loadError()) {
          <p class="server-error" role="alert">{{ 'common.loadError' | translate }}</p>
        } @else if ((list()?.items ?? []).length === 0) {
          <p class="help">{{ 'admin.apiKeys.none' | translate }}</p>
        } @else {
          <!-- DEF-07 (WCAG 2.1.1): scrollable table as a focusable region with a name -->
          <div class="table-wrap" tabindex="0" role="region" [attr.aria-label]="'admin.apiKeys.listTitle' | translate">
            <table>
              <caption class="sr-only">{{ 'admin.apiKeys.listTitle' | translate }}</caption>
              <thead><tr>
                <th scope="col">{{ 'admin.apiKeys.col.name' | translate }}</th>
                <th scope="col">{{ 'admin.apiKeys.col.clientId' | translate }}</th>
                <th scope="col">{{ 'admin.apiKeys.col.created' | translate }}</th>
                <th scope="col">{{ 'admin.apiKeys.col.status' | translate }}</th>
                <th scope="col"><span class="sr-only">{{ 'admin.apiKeys.col.actions' | translate }}</span></th>
              </tr></thead>
              <tbody>
                @for (k of list()!.items; track k.id) {
                  <tr [class.revoked]="!k.active">
                    <td [attr.data-label]="'admin.apiKeys.col.name' | translate"><strong>{{ k.name }}</strong></td>
                    <td [attr.data-label]="'admin.apiKeys.col.clientId' | translate"><code>{{ k.clientId }}</code></td>
                    <td [attr.data-label]="'admin.apiKeys.col.created' | translate">{{ k.createdAt | localDate: language() }}
                      @if (k.createdByName) { <br /><small>{{ k.createdByName }}</small> }</td>
                    <td [attr.data-label]="'admin.apiKeys.col.status' | translate">
                      @if (k.active) { <span class="pill active">{{ 'admin.apiKeys.active' | translate }}</span> }
                      @else { <span class="pill">{{ 'admin.apiKeys.revokedOn' | translate: { date: (k.revokedAt | localDate: language()) } }}</span> }</td>
                    <td>
                      @if (k.active) {
                        @if (confirming() === k.id) {
                          <span class="confirm">{{ 'admin.apiKeys.confirmRevoke' | translate }}
                            <ion-button size="small" color="danger" (click)="revoke(k)" [disabled]="busy()">{{ 'admin.apiKeys.yesRevoke' | translate }}</ion-button>
                            <ion-button size="small" fill="outline" (click)="confirming.set(null)">{{ 'admin.apiKeys.no' | translate }}</ion-button></span>
                        } @else {
                          <ion-button size="small" fill="clear" color="danger" (click)="confirming.set(k.id)"
                                      [attr.aria-label]="'admin.apiKeys.revokeLabel' | translate: { name: k.name }">
                            {{ 'admin.apiKeys.revoke' | translate }}</ion-button>
                        }
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
        @if (message(); as m) { <p class="ok" role="status">{{ m }}</p> }
        @if (listError(); as e) { <p class="server-error" role="alert">{{ e }}</p> }
      </section>
    </app-admin-page>
  `,
  styles: `
    .intro { margin: 0 0 16px; color: var(--ion-color-medium); max-width: 680px; }
    .help { color: var(--ion-color-medium); font-size: 0.875rem; }
    .server-error { color: var(--ion-color-danger); font-weight: 600; }
    .ok { color: var(--ion-color-success); font-weight: 600; }
    .new { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-end; }
    .new label { display: flex; flex-direction: column; gap: 4px; font-weight: 600; font-size: 0.875rem; flex: 1 1 260px; min-width: 0; }
    .new input { min-height: 48px; padding: 0 12px; font: inherit; font-weight: 400; font-size: 1rem; color: var(--ion-text-color, #1b2733);
      background: #fff; border: 1px solid #9aa8b8; border-radius: var(--vv-radius-control, 8px); width: 100%; box-sizing: border-box; }
    .new input:focus-visible { outline: 3px solid var(--ion-color-primary); outline-offset: 2px; }
    .new ion-button { margin: 0; min-height: 48px; }
    details { margin-top: 8px; }
    summary { cursor: pointer; font-weight: 600; min-height: 44px; display: flex; align-items: center; }
    .scope { margin: 4px 0; padding-left: 20px; }
    .secret { border: 2px solid var(--ion-color-primary); }
    .secret h3 ion-icon { vertical-align: -3px; }
    .warning { display: flex; gap: 8px; align-items: flex-start; background: #fff7e0; border: 1px solid #f0d58a; color: #6b4e00;
      border-radius: var(--vv-radius-control, 8px); padding: 12px 14px; margin: 0 0 12px; font-weight: 600; }
    .warning ion-icon { flex: none; margin-top: 3px; }
    .creds { display: grid; gap: 4px; margin: 0 0 8px; }
    .creds div { display: flex; flex-wrap: wrap; gap: 4px 12px; align-items: center; border-bottom: 1px solid var(--vv-border-color); padding: 4px 0; }
    .creds dt { color: #4a5868; flex: 0 0 160px; }
    .creds dd { margin: 0; display: flex; align-items: center; gap: 4px; min-width: 0; flex: 1 1 200px; }
    code { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 0.875rem; overflow-wrap: anywhere; }
    .secret-value { background: #f2f4f7; padding: 2px 6px; border-radius: 4px; }
    .example { background: #1b2733; color: #e8eef6; padding: 12px; border-radius: 8px; overflow-x: auto; font-size: 0.8125rem; white-space: pre-wrap; overflow-wrap: anywhere; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; font-size: 0.9375rem; }
    th { text-align: left; font-size: 0.8125rem; text-transform: uppercase; letter-spacing: 0.02em; color: #4a5868; background: #f2f4f7; padding: 10px 12px; }
    td { padding: 10px 12px; border-top: 1px solid var(--vv-border-color); vertical-align: top; }
    td small { color: var(--ion-color-medium); }
    tr.revoked td { color: var(--ion-color-medium); }
    .pill { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 0.8125rem; font-weight: 600; background: #e3e8ef; color: #34404d; }
    .pill.active { background: #e4f4ea; color: #1d6b3a; }
    .confirm { display: inline-flex; flex-wrap: wrap; gap: 6px; align-items: center; font-weight: 600; }
    @media (max-width: 767px) {
      thead { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
      table, tbody, tr, td { display: block; }
      tr { border-top: 1px solid var(--vv-border-color); padding: 10px 0; }
      td { border: 0; padding: 2px 0; display: flex; gap: 8px; flex-wrap: wrap; }
      td[data-label]::before { content: attr(data-label); flex: 0 0 35%; color: var(--ion-color-medium); font-size: 0.875rem; }
    }
  `,
})
export class ApiKeysPage {
  private readonly api = inject(ApiKeysApiService);
  private readonly translate = inject(TranslateService);
  protected readonly language = inject(LanguageService).current;
  protected readonly list = signal<ApiKeyList | null>(null);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly name = signal('');
  protected readonly created = signal<CreatedApiKey | null>(null);
  protected readonly copied = signal<'id' | 'secret' | null>(null);
  protected readonly confirming = signal<number | null>(null);
  protected readonly formError = signal<string | null>(null);
  protected readonly listError = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly activeCount = computed(() => (this.list()?.items ?? []).filter((key) => key.active).length);
  protected readonly full = computed(() => this.activeCount() >= (this.list()?.maxActive ?? 5));

  ionViewWillEnter(): void {
    void this.load(true);
  }

  private async load(spinner: boolean): Promise<void> {
    if (spinner) this.loading.set(true);
    try {
      this.list.set(await this.api.list());
      this.loadError.set(false);
    } catch {
      this.loadError.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  protected async create(): Promise<void> {
    const name = this.name().trim();
    if (!name) return;
    this.busy.set(true);
    this.formError.set(null);
    this.message.set(null);
    this.copied.set(null);
    try {
      this.created.set(await this.api.create(name));
      this.name.set('');
      await this.load(false);
    } catch (e) {
      this.formError.set(this.problem(e));
    } finally {
      this.busy.set(false);
    }
  }

  protected async revoke(key: ApiKey): Promise<void> {
    this.busy.set(true);
    this.listError.set(null);
    try {
      await this.api.revoke(key.id);
      this.confirming.set(null);
      await this.load(false);
      this.message.set(this.translate.instant('admin.apiKeys.revoked', { name: key.name }) as string);
    } catch (e) {
      this.listError.set(this.problem(e));
    } finally {
      this.busy.set(false);
    }
  }

  protected async copy(value: string, what: 'id' | 'secret'): Promise<void> {
    try {
      await navigator.clipboard.writeText(value);
      this.copied.set(what);
    } catch {
      this.copied.set(null);
    }
  }

  protected example(c: CreatedApiKey): string {
    return [
      `curl -s -X POST ${c.tokenUrl} \\`,
      `  -d grant_type=client_credentials -d client_id=${c.key.clientId} -d client_secret=<secreto>`,
      '',
      `curl -s -H "Authorization: Bearer <access_token>" ${APP_CONFIG.apiBaseUrl || window.location.origin}/api/v1/processes`,
    ].join('\n');
  }

  private problem(e: unknown): string {
    if (e instanceof ApiError && e.detail && e.status !== 401) return e.detail;
    return this.translate.instant('common.loadError') as string;
  }
}
