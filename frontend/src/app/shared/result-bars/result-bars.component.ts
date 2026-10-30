import { Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { Result } from '../../core/results/results.model';
import { LocalizedPipe } from '../pipes/localized.pipe';

interface Row {
  key: string;
  label: string | null;
  text: Result['options'][number]['text'] | null;
  votes: number;
  percentage: number;
  top: boolean;
}

/**
 * Result per option (F1, V8): one horizontal bar per option, most voted first, blank vote last. One series in one hue,
 * value written next to each bar, so identity never depends on color (WCAG 1.4.1).
 */
@Component({
  selector: 'app-result-bars',
  imports: [TranslatePipe, LocalizedPipe],
  template: `
    <ul class="rows">
      @for (row of rows(); track row.key) {
        <li [class.blank]="!row.text" [title]="(row.text ? (row.text | localized: language()) : (row.label! | translate)) + ': ' + row.votes + ' · ' + pct(row.percentage) + ' %'">
          <div class="line">
            <span class="name">{{ row.text ? (row.text | localized: language()) : (row.label! | translate) }}
              @if (row.top) { <span class="top">{{ 'results.top' | translate }}</span> }</span>
            <span class="value"><strong>{{ n(row.votes) }}</strong> {{ (row.votes === 1 ? 'results.vote' : 'results.votes') | translate }} · {{ pct(row.percentage) }} %</span>
          </div>
          <span class="track" aria-hidden="true"><span class="bar" [style.width.%]="row.percentage"></span></span>
        </li>
      }
    </ul>
  `,
  styles: `
    .rows { list-style: none; margin: 0; padding: 0; display: grid; gap: 16px; }
    .line { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 4px 12px; margin-bottom: 6px; }
    .name { font-weight: 600; display: inline-flex; flex-wrap: wrap; gap: 8px; align-items: center; }
    .value { color: #4a5868; white-space: nowrap; }
    .value strong { color: var(--ion-text-color, #1b2733); }
    .top { font-size: 0.8125rem; font-weight: 600; padding: 1px 8px; border-radius: 999px; border: 1px solid currentColor;
      color: #14573f; background: #e6f4ee; }
    .track { display: block; height: 10px; border-radius: 999px; background: #e3e8ef; overflow: hidden; }
    .bar { display: block; height: 100%; min-width: 2px; border-radius: 999px; background: #5a7290; }
    li:first-child .bar { background: var(--ion-color-primary); }
    .blank .bar { background: #8a97a8; }
  `,
})
export class ResultBarsComponent {
  readonly result = input.required<Result>();
  readonly language = input.required<string>();

  protected readonly rows = computed<Row[]>(() => {
    const r = this.result();
    const max = Math.max(0, ...r.options.map((o) => o.votes));
    const options = [...r.options]
      .sort((a, b) => b.votes - a.votes || a.position - b.position)
      .map((o) => ({ key: 'o' + o.optionId, label: null, text: o.text, votes: o.votes, percentage: +o.percentage,
        top: max > 0 && o.votes === max }));
    return r.blankVoteEnabled
      ? [...options, { key: 'blank', label: 'results.blank', text: null, votes: r.blankVotes, percentage: +r.blankPercentage, top: false }]
      : options;
  });

  protected n(value: number): string {
    return new Intl.NumberFormat(this.language()).format(value);
  }

  protected pct(value: number): string {
    return new Intl.NumberFormat(this.language(), { minimumFractionDigits: 1, maximumFractionDigits: 2 }).format(value);
  }
}
