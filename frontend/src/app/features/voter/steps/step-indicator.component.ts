import { Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

/** «Paso N de 4» with four bars (RNF-9). The welcome screen is step 0 and shows no number. */
@Component({
  selector: 'app-step-indicator',
  imports: [TranslatePipe],
  template: `
    <nav class="steps" [attr.aria-label]="'voter.steps.label' | translate">
      <p class="count">{{ 'voter.steps.count' | translate: { current: step(), total: 4 } }}</p>
      <ol>
        @for (key of keys; track key; let index = $index) {
          <li [class.done]="index + 1 < step()" [class.current]="index + 1 === step()"
              [attr.aria-current]="index + 1 === step() ? 'step' : null">
            <span class="bar" aria-hidden="true"></span>
            <span class="name">{{ 'voter.steps.' + key | translate }}</span>
          </li>
        }
      </ol>
    </nav>
  `,
  styles: `
    .steps { margin: 0 0 1.25rem; }
    .count { margin: 0 0 0.5rem; font-size: 0.875rem; color: var(--ion-color-medium); }
    ol { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; }
    li { font-size: 0.875rem; color: var(--ion-color-medium); }
    .bar { display: block; height: 6px; border-radius: 3px; background: var(--vv-border-color); margin-bottom: 4px; }
    li.done .bar, li.current .bar { background: var(--ion-color-primary); }
    li.current { color: var(--ion-text-color); font-weight: 600; }
    @media (max-width: 360px) { .name { display: none; } }
  `,
})
export class StepIndicatorComponent {
  readonly step = input.required<number>();
  protected readonly keys = ['access', 'selection', 'confirmation', 'receipt'] as const;
  protected readonly visible = computed(() => this.step() > 0);
}
