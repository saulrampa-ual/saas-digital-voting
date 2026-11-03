import { Component, computed, input } from '@angular/core';

/** One calendar day of cumulative participation (D-18: days only, never a time). */
export interface DayChartPoint {
  /** `YYYY-MM-DD`. */
  readonly day: string;
  /** People who have voted up to that day. */
  readonly cumulative: number;
  /** `cumulative` over the census, 0–100. */
  readonly pct: number;
}

interface Column {
  readonly day: string;
  /** null for the days that have not happened yet (inside the voting period). */
  readonly pct: number | null;
  readonly dayLabel: string;
  /** Month name under the first day shown and under every first day of a month. */
  readonly monthLabel: string | null;
  readonly showDay: boolean;
  /** Day and month, for the tooltip. */
  readonly tip: string;
  readonly showValue: boolean;
  readonly today: boolean;
}

/** Gridlines of the fixed 0–100 % scale: the bars are read against the whole census, not against the highest one. */
const TICKS = [0, 25, 50, 75, 100] as const;
/** Longest period drawn day by day (the most recent days are kept). */
const MAX_DAYS = 62;
/** Above this number of columns only some day numbers are written, so they do not overlap. */
const LABELLED_DAYS = 16;

/**
 * Cumulative participation per day, drawn over the whole voting period (from the opening day to the closing day) so a
 * single day of votes is seen in context: a fixed 0–100 % scale with gridlines, every day of the period (the future
 * ones empty) and the month names below the axis. Purely visual (aria-hidden): each page keeps its hidden table with
 * the same values for screen readers.
 */
@Component({
  selector: 'app-day-chart',
  template: `
    <div class="chart" aria-hidden="true">
      <div class="y-axis">
        @for (t of ticks; track t) { <span [style.bottom.%]="t">{{ t }} %</span> }
      </div>
      <div class="scroll">
        <div class="area" [style.min-width.px]="columns().length * 22">
          <div class="plot">
            @for (t of ticks; track t) { <span class="grid" [style.bottom.%]="t"></span> }
            <div class="cols">
              @for (c of columns(); track c.day) {
                <div class="col" [class.future]="c.pct === null" [attr.title]="c.pct === null ? null : c.tip + ': ' + format(c.pct) + ' %'">
                  @if (c.pct !== null) {
                    <i class="bar" [style.height.%]="c.pct"></i>
                    @if (c.showValue) { <span class="value" [style.bottom.%]="c.pct">{{ format(c.pct) }} %</span> }
                  }
                </div>
              }
            </div>
          </div>
          <div class="cols x-days">
            @for (c of columns(); track c.day) {
              <span [class.today]="c.today">{{ c.showDay || c.today ? c.dayLabel : '' }}</span>
            }
          </div>
          <div class="cols x-months">
            @for (c of columns(); track c.day) { <span>{{ c.monthLabel ?? '' }}</span> }
          </div>
        </div>
      </div>
    </div>
  `,
  styles: `
    :host { display: block; --plot-height: 200px; --muted: #4b5563; }
    .chart { display: grid; grid-template-columns: 3.25rem minmax(0, 1fr); }
    .y-axis { position: relative; height: var(--plot-height); margin-top: 22px; }
    .y-axis span {
      position: absolute; right: 8px; transform: translateY(50%); font-size: 0.75rem; color: var(--muted);
      font-variant-numeric: tabular-nums; white-space: nowrap;
    }
    .scroll { overflow-x: auto; overflow-y: hidden; }
    /* Room for the value written over the first and last bars, which is wider than a narrow column. */
    .area { padding-inline: 16px; }
    .plot { position: relative; height: var(--plot-height); margin-top: 22px; border-bottom: 1px solid #7b8794; }
    .grid { position: absolute; left: 0; right: 0; border-top: 1px dashed #d5dce5; }
    .cols { display: grid; grid-auto-flow: column; grid-auto-columns: minmax(0, 1fr); gap: 4px; }
    .plot .cols { position: absolute; inset: 0; }
    .col { position: relative; }
    .col.future { background: repeating-linear-gradient(135deg, transparent 0 6px, rgba(var(--ion-color-primary-rgb), 0.05) 6px 9px); }
    .bar {
      position: absolute; bottom: 0; left: 50%; transform: translateX(-50%); width: min(80%, 48px); min-height: 2px;
      border-radius: 4px 4px 0 0; background: var(--ion-color-primary);
    }
    .col:hover .bar { background: var(--ion-color-secondary); }
    .value {
      position: absolute; left: 50%; transform: translate(-50%, -4px); font-size: 0.8125rem; font-weight: 700;
      color: var(--ion-color-primary); white-space: nowrap; font-variant-numeric: tabular-nums;
    }
    .x-days span { padding-top: 4px; text-align: center; font-size: 0.75rem; color: var(--muted); font-variant-numeric: tabular-nums; }
    .x-days .today { font-weight: 700; color: var(--ion-color-primary); }
    .x-months span { overflow: visible; white-space: nowrap; font-size: 0.8125rem; font-weight: 600; color: #1f2937; }
    @media (max-width: 480px) { :host { --plot-height: 160px; } }
  `,
})
export class DayChartComponent {
  /** Days with votes (they may skip days; a missing day keeps the previous cumulative value). */
  readonly points = input.required<readonly DayChartPoint[]>();
  /** Start and end of the voting period (ISO date-times); the chart covers every day in between. */
  readonly from = input<string | null>(null);
  readonly to = input<string | null>(null);
  readonly language = input.required<string>();

  protected readonly ticks = TICKS;

  protected readonly columns = computed<Column[]>(() => {
    const points = [...this.points()].sort((a, b) => a.day.localeCompare(b.day));
    const today = localDay(new Date());
    const first = [this.from() ? localDay(new Date(this.from()!)) : null, points[0]?.day].filter(isDay).sort()[0];
    const last = [this.to() ? localDay(new Date(this.to()!)) : null, points.at(-1)?.day].filter(isDay).sort().at(-1);
    if (!first || !last) return [];
    const days = enumerateDays(first, last).slice(-MAX_DAYS);
    const step = Math.ceil(days.length / LABELLED_DAYS);
    const dayFormat = new Intl.DateTimeFormat(this.language(), { day: 'numeric', timeZone: 'UTC' });
    const monthFormat = new Intl.DateTimeFormat(this.language(), { month: 'long', timeZone: 'UTC' });
    const tipFormat = new Intl.DateTimeFormat(this.language(), { day: 'numeric', month: 'long', timeZone: 'UTC' });
    const byDay = new Map(points.map((p) => [p.day, p.pct]));
    let carried = points.filter((p) => p.day < days[0]).at(-1)?.pct ?? 0;
    let previous: number | null = null;
    /** Last day already drawn: with many columns only its value is written. */
    const lastDrawn = days.filter((d) => d <= today || byDay.has(d)).at(-1);
    return days.map((day, index) => {
      const date = new Date(`${day}T12:00:00Z`);
      const future = day > today && !byDay.has(day);
      if (byDay.has(day)) carried = byDay.get(day)!;
      const pct = future ? null : carried;
      const changed = pct !== null && pct !== previous && pct > 0;
      if (pct !== null) previous = pct;
      return {
        day,
        pct,
        dayLabel: dayFormat.format(date),
        monthLabel: index === 0 || day.endsWith('-01') ? monthFormat.format(date) : null,
        showDay: index % step === 0,
        tip: tipFormat.format(date),
        showValue: days.length <= LABELLED_DAYS ? changed : pct !== null && day === lastDrawn,
        today: day === today,
      };
    });
  });

  protected format(value: number): string {
    return new Intl.NumberFormat(this.language(), { maximumFractionDigits: 1 }).format(value);
  }
}

function isDay(value: string | null | undefined): value is string {
  return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value);
}

/** Calendar day in the browser's time zone, `YYYY-MM-DD`. */
function localDay(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function enumerateDays(first: string, last: string): string[] {
  const days: string[] = [];
  for (let t = Date.parse(`${first}T00:00:00Z`), end = Date.parse(`${last}T00:00:00Z`); t <= end; t += 86_400_000) {
    days.push(new Date(t).toISOString().slice(0, 10));
  }
  return days;
}
