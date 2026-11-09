import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { DayChartComponent, DayChartPoint } from './day-chart.component';

/**
 * P7.1 (A5): participation per day (D-18: days only, never a time). The chart is purely visual (aria-hidden); these
 * tests check the columns it draws: the whole voting period, carried values on days without votes, empty future days,
 * the fixed 0–100 % scale and the day and month labels.
 */
describe('DayChartComponent', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 10, 12, 12, 0, 0)); // 12 November 2026, local time
  });
  afterEach(() => vi.useRealTimers());

  function render(points: DayChartPoint[], from: string | null, to: string | null, language = 'es') {
    const fixture = TestBed.createComponent(DayChartComponent);
    fixture.componentRef.setInput('points', points);
    fixture.componentRef.setInput('from', from);
    fixture.componentRef.setInput('to', to);
    fixture.componentRef.setInput('language', language);
    fixture.detectChanges();
    const root: HTMLElement = fixture.nativeElement;
    const cols = [...root.querySelectorAll<HTMLElement>('.plot .col')];
    return {
      root,
      cols,
      heights: cols.map((c) => c.querySelector<HTMLElement>('.bar')?.style.height ?? null),
      future: cols.map((c) => c.classList.contains('future')),
      days: [...root.querySelectorAll<HTMLElement>('.x-days span')].map((s) => s.textContent!.trim()),
      months: [...root.querySelectorAll<HTMLElement>('.x-months span')].map((s) => s.textContent!.trim()),
      values: [...root.querySelectorAll<HTMLElement>('.value')].map((s) => s.textContent!.trim()),
    };
  }

  it('draws every day of the voting period, including the future ones as empty columns', () => {
    const chart = render(
      [{ day: '2026-11-09', cumulative: 2, pct: 20 }, { day: '2026-11-11', cumulative: 5, pct: 50 }],
      '2026-11-09T09:00:00', '2026-11-14T18:00:00',
    );
    expect(chart.cols).toHaveLength(6); // 9 to 14 November
    expect(chart.future).toEqual([false, false, false, false, true, true]);
    expect(chart.heights.slice(4)).toEqual([null, null]);
  });

  it('keeps the previous cumulative value on days without votes', () => {
    const chart = render(
      [{ day: '2026-11-09', cumulative: 2, pct: 20 }, { day: '2026-11-11', cumulative: 5, pct: 50 }],
      '2026-11-09T09:00:00', '2026-11-12T18:00:00',
    );
    expect(chart.heights).toEqual(['20%', '20%', '50%', '50%']);
  });

  it('starts at 0 % before the first vote', () => {
    const chart = render([{ day: '2026-11-11', cumulative: 1, pct: 10 }], '2026-11-09T09:00:00', '2026-11-12T18:00:00');
    expect(chart.heights).toEqual(['0%', '0%', '10%', '10%']);
  });

  it('uses a fixed 0–100 % scale: bar heights are the participation, not relative to the highest bar', () => {
    const chart = render([{ day: '2026-11-12', cumulative: 1, pct: 12.5 }], '2026-11-12T09:00:00', '2026-11-12T18:00:00');
    expect(chart.heights).toEqual(['12.5%']);
    const ticks = [...chart.root.querySelectorAll('.y-axis span')].map((s) => s.textContent!.trim());
    expect(ticks).toEqual(['0 %', '25 %', '50 %', '75 %', '100 %']);
  });

  it('writes the value only where it changes, formatted for the language', () => {
    const points = [{ day: '2026-11-10', cumulative: 1, pct: 12.5 }, { day: '2026-11-11', cumulative: 3, pct: 37.5 }];
    expect(render(points, '2026-11-10T09:00:00', '2026-11-12T18:00:00', 'es').values).toEqual(['12,5 %', '37,5 %']);
    expect(render(points, '2026-11-10T09:00:00', '2026-11-12T18:00:00', 'en').values).toEqual(['12.5 %', '37.5 %']);
  });

  it('writes the month under the first day and under every first day of a month', () => {
    const chart = render([{ day: '2026-11-30', cumulative: 1, pct: 10 }], '2026-11-29T09:00:00', '2026-12-02T18:00:00', 'es');
    expect(chart.months).toEqual(['noviembre', '', 'diciembre', '']);
    expect(chart.days).toEqual(['29', '30', '1', '2']);
  });

  it('draws at most the last 62 days of a long period', () => {
    const chart = render([{ day: '2026-11-12', cumulative: 1, pct: 5 }], '2026-07-01T09:00:00', '2026-11-12T18:00:00');
    expect(chart.cols).toHaveLength(62);
    expect(chart.cols.at(-1)!.getAttribute('title')).toContain('12');
  });

  it('draws nothing without a period or votes', () => {
    expect(render([], null, null).cols).toHaveLength(0);
  });

  it('is hidden from screen readers (each page keeps its own data table)', () => {
    const chart = render([{ day: '2026-11-12', cumulative: 1, pct: 5 }], '2026-11-12T09:00:00', '2026-11-12T18:00:00');
    expect(chart.root.querySelector('.chart')!.getAttribute('aria-hidden')).toBe('true');
  });
});
