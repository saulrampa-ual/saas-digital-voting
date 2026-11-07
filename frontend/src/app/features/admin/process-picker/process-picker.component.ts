import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../core/i18n/language.service';
import { ProcessApiService, ProcessStatus, ProcessSummary } from '../../../core/admin/process-api.service';
import { LocalizedPipe } from '../../../shared/pipes/localized.pipe';
import { StatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';

/**
 * «Proceso» selector of A5, F1 and H1: the processes of the organization in the statuses that make sense for the page,
 * newest first. When nothing is selected yet, it chooses the first one.
 */
@Component({
  selector: 'app-process-picker',
  imports: [TranslatePipe, LocalizedPipe, StatusBadgeComponent],
  template: `
    @if (loaded() && options().length === 0) {
      <p class="empty">{{ emptyKey() | translate }}</p>
    } @else {
      <label class="picker">
        <span>{{ 'admin.picker.label' | translate }}</span>
        <select (change)="selectedChange.emit(+$any($event.target).value)">
          @for (p of options(); track p.id) {
            <option [value]="p.id" [selected]="p.id === selected()">{{ p.title | localized: language() }} · {{ 'admin.processes.status.' + p.status | translate }}</option>
          }
        </select>
      </label>
      <!-- SR3/SR10: the native select cuts long names on phones; the chosen process is also shown in full -->
      @if (showCurrent() && current(); as c) {
        <h2 class="current"><span>{{ c.title | localized: language() }}</span> <app-status-badge [status]="c.status" /></h2>
      }
    }
  `,
  styles: `
    :host { display: block; min-width: 0; }
    .picker { display: flex; flex-direction: column; gap: 4px; min-width: 0; width: 100%; max-width: 420px; font-weight: 600; font-size: 0.875rem; }
    select { width: 100%; min-width: 0; max-width: 100%; text-overflow: ellipsis; min-height: 48px; padding: 0 12px; font: inherit; font-weight: 400; font-size: 1rem; color: var(--ion-text-color, #1b2733);
      background: #fff; border: 1px solid #9aa8b8; border-radius: var(--vv-radius-control, 8px); }
    select:focus-visible { outline: 3px solid var(--ion-color-primary); outline-offset: 2px; }
    @media (max-width: 767.98px) { .picker { max-width: none; } }
    .empty { color: var(--ion-color-medium); margin: 0; }
    .current { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 10px; margin: 12px 0 0; font-size: 1.125rem; font-weight: 650;
      line-height: 1.3; color: var(--ion-text-color); }
  `,
})
export class ProcessPickerComponent implements OnInit {
  readonly statuses = input.required<readonly ProcessStatus[]>();
  readonly selected = input<number | null>(null);
  readonly emptyKey = input('admin.picker.empty');
  /** Shows the chosen process (full name and status) under the select; for pages that do not repeat it. */
  readonly showCurrent = input(false);
  readonly selectedChange = output<number>();

  private readonly api = inject(ProcessApiService);
  protected readonly language = inject(LanguageService).current;
  private readonly all = signal<ProcessSummary[]>([]);
  protected readonly loaded = signal(false);
  protected readonly options = computed(() => this.all().filter((p) => this.statuses().includes(p.status)));
  protected readonly current = computed(() => this.options().find((p) => p.id === this.selected()) ?? null);

  async ngOnInit(): Promise<void> {
    try {
      this.all.set(await this.api.list());
    } finally {
      this.loaded.set(true);
    }
    const options = this.options();
    if (options.length && !options.some((p) => p.id === this.selected())) {
      this.selectedChange.emit(options[0].id);
    }
  }
}
