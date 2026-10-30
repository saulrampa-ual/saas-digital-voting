import { Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ProcessStatus } from '../../core/admin/process-api.service';

/** Status label with text and color (never color alone, WCAG 1.4.1). */
@Component({
  selector: 'app-status-badge',
  imports: [TranslatePipe],
  template: `<span class="badge" [class]="'badge ' + tone()">{{ 'admin.processes.status.' + status() | translate }}</span>`,
  styles: `
    .badge { display: inline-block; font-size: 0.875rem; font-weight: 600; padding: 2px 10px; border-radius: 999px;
      border: 1px solid currentColor; white-space: nowrap; }
    .neutral { color: #4a5868; background: #eef1f5; }
    .info { color: #14365c; background: #e8eef6; }
    .ok { color: #14573f; background: #e6f4ee; }
    .warn { color: #7a4b00; background: #fff4dc; }
  `,
})
export class StatusBadgeComponent {
  readonly status = input.required<ProcessStatus>();
  protected readonly tone = computed(() => {
    switch (this.status()) {
      case 'SCHEDULED': return 'info';
      case 'OPEN': case 'TALLIED': return 'ok';
      case 'CLOSED': return 'warn';
      default: return 'neutral';
    }
  });
}
