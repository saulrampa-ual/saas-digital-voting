import { Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { Result } from '../../core/results/results.model';
import { ResultBarsComponent } from '../result-bars/result-bars.component';
import { ResultVerificationComponent, VerificationState } from '../result-verification/result-verification.component';

/**
 * Result as a voter sees it (V8 public link and, with the personal link, D-41): votes, turnout, the result per option
 * and the verification made by the browser.
 */
@Component({
  selector: 'app-result-summary',
  imports: [TranslatePipe, ResultBarsComponent, ResultVerificationComponent],
  template: `
    <div class="figures">
      <div><strong>{{ n(result().totalVotes) }}</strong><span>{{ 'publicResults.votes' | translate }}</span></div>
      <div><strong>{{ pct(result().participation) }} %</strong><span>{{ 'publicResults.turnout' | translate: { census: n(result().eligibleVoters) } }}</span></div>
    </div>
    <section class="block">
      <h2>{{ 'publicResults.byOption' | translate }}</h2>
      <app-result-bars [result]="result()" [language]="language()" />
    </section>
    <app-result-verification [state]="verification()" [resultHash]="result().resultHash" [finalHash]="result().finalHash" />
  `,
  styles: `
    :host { display: block; }
    .figures { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 1.25rem; }
    .figures div { background: #f2f4f7; border-radius: var(--vv-radius-card); padding: 12px 16px; display: flex; flex-direction: column; }
    .figures strong { font-size: 1.75rem; color: var(--ion-color-primary); line-height: 1.1; }
    .figures span { font-size: 0.875rem; color: #4a5868; }
    .block { margin-bottom: 1.25rem; }
    .block h2 { font-size: 1.125rem; margin: 0 0 12px; }
  `,
})
export class ResultSummaryComponent {
  readonly result = input.required<Result>();
  readonly language = input.required<string>();
  readonly verification = input.required<VerificationState>();

  protected n(value: number): string {
    return new Intl.NumberFormat(this.language()).format(value);
  }

  protected pct(value: number): string {
    return new Intl.NumberFormat(this.language(), { maximumFractionDigits: 2 }).format(+value);
  }
}
