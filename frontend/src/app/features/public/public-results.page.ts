import { Component, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonButtons, IonContent, IonHeader, IonSpinner, IonTitle, IonToolbar } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { ApiError } from '../../core/admin/api-error';
import { LanguageService } from '../../core/i18n/language.service';
import { PublicResultsService } from '../../core/results/public-results.service';
import { verifyResult } from '../../core/results/result-verifier';
import { Result } from '../../core/results/results.model';
import { ErrorStateComponent } from '../../shared/error-state/error-state.component';
import { LanguageSwitcherComponent } from '../../shared/language-switcher/language-switcher.component';
import { LocalDatePipe } from '../../shared/pipes/local-date.pipe';
import { LocalizedPipe } from '../../shared/pipes/localized.pipe';
import { ResultBarsComponent } from '../../shared/result-bars/result-bars.component';
import {
  ResultVerificationComponent, VerificationState,
} from '../../shared/result-verification/result-verification.component';

/**
 * V8 (RF-13, RF-15): result through the public link of a `PUBLIC_LINK` process (D-16), once tallied. No account and
 * no token: the random link is the only key. The browser recomputes the result from the published votes (D-37).
 */
@Component({
  selector: 'app-public-results',
  imports: [
    IonHeader, IonToolbar, IonTitle, IonButtons, IonContent, IonSpinner, RouterLink, TranslatePipe, LocalDatePipe,
    LocalizedPipe, LanguageSwitcherComponent, ErrorStateComponent, ResultBarsComponent, ResultVerificationComponent,
  ],
  template: `
    <ion-header>
      <ion-toolbar color="tertiary">
        <ion-title>VeriVoto</ion-title>
        <ion-buttons slot="end"><app-language-switcher /></ion-buttons>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <main class="voter">
        @if (loading()) {
          <ion-spinner role="status" [attr.aria-label]="'common.loading' | translate" />
        } @else if (notFound()) {
          <app-error-state icon="link-outline" titleKey="publicResults.notFoundTitle" messageKey="publicResults.notFoundText" />
        } @else if (failed()) {
          <app-error-state icon="cloud-offline-outline" titleKey="publicResults.errorTitle" messageKey="common.loadError" />
        } @else if (result(); as r) {
          <p class="eyebrow">{{ 'publicResults.eyebrow' | translate }}</p>
          <h1 tabindex="-1">{{ r.question | localized: language() }}</h1>
          <p class="meta">{{ 'publicResults.published' | translate: { date: (r.talliedAt | localDate: language()) } }}</p>
          <div class="figures">
            <div><strong>{{ n(r.totalVotes) }}</strong><span>{{ 'publicResults.votes' | translate }}</span></div>
            <div><strong>{{ pct(r.participation) }} %</strong><span>{{ 'publicResults.turnout' | translate: { census: n(r.eligibleVoters) } }}</span></div>
          </div>
          <section class="block">
            <h2>{{ 'publicResults.byOption' | translate }}</h2>
            <app-result-bars [result]="r" [language]="language()" />
          </section>
          <app-result-verification [state]="verification()" [resultHash]="r.resultHash" [finalHash]="r.finalHash" />
          <p class="receipt"><a [routerLink]="['/recibo', r.processId]">{{ 'publicResults.checkReceipt' | translate }}</a></p>
        }
      </main>
    </ion-content>
  `,
  styles: `
    .voter { max-width: 40rem; margin: 0 auto; }
    .eyebrow { margin: 0.5rem 0 0; color: var(--ion-color-medium); font-weight: 600; font-size: 0.875rem; text-transform: uppercase; letter-spacing: 0.04em; }
    h1 { margin: 4px 0; }
    .meta { color: var(--ion-color-medium); margin: 0 0 1rem; }
    .figures { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 1.25rem; }
    .figures div { background: #f2f4f7; border-radius: var(--vv-radius-card); padding: 12px 16px; display: flex; flex-direction: column; }
    .figures strong { font-size: 1.75rem; color: var(--ion-color-primary); line-height: 1.1; }
    .figures span { font-size: 0.875rem; color: #4a5868; }
    .block { margin-bottom: 1.25rem; }
    .block h2 { font-size: 1.125rem; margin: 0 0 12px; }
    .receipt { margin-top: 1.25rem; }
    .receipt a { display: inline-block; min-height: 44px; line-height: 44px; color: var(--ion-color-primary); font-weight: 600; }
  `,
})
export class PublicResultsPage {
  readonly publicId = input.required<string>();

  private readonly api = inject(PublicResultsService);
  protected readonly language = inject(LanguageService).current;
  protected readonly result = signal<Result | null>(null);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly failed = signal(false);
  protected readonly verification = signal<VerificationState>('checking');

  async ionViewWillEnter(): Promise<void> {
    this.loading.set(true);
    this.notFound.set(false);
    this.failed.set(false);
    try {
      const result = await this.api.result(this.publicId());
      this.result.set(result);
      void this.verify(result);
    } catch (error) {
      if (error instanceof ApiError && (error.status === 404 || error.status === 400)) this.notFound.set(true);
      else this.failed.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  private async verify(result: Result): Promise<void> {
    try {
      const verification = await verifyResult(await this.api.countedVotes(this.publicId()), result);
      this.verification.set(verification.matches ? 'ok' : 'mismatch');
    } catch {
      this.verification.set('error');
    }
  }

  protected n(value: number): string {
    return new Intl.NumberFormat(this.language()).format(value);
  }

  protected pct(value: number): string {
    return new Intl.NumberFormat(this.language(), { maximumFractionDigits: 2 }).format(+value);
  }
}
