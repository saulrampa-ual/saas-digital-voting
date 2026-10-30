import { Component, input, model, output, signal } from '@angular/core';
import { IonButton, IonItem, IonLabel, IonList, IonRadio, IonRadioGroup, IonText } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { Ballot } from '../../../core/voter/voter-api.service';
import { LocalizedPipe } from '../../../shared/pipes/localized.pipe';

/** Value of the radio group: an option id, or 'blank'. */
export type BallotChoice = number | 'blank' | null;

/**
 * B2: options as large cards (64 px) and the blank vote if the ballot allows it. Continuing without a choice shows
 * the error on the same screen (C1). The choice lives in the parent, so changing language keeps it (RF-20).
 */
@Component({
  selector: 'app-ballot-step',
  imports: [IonButton, IonItem, IonLabel, IonList, IonRadio, IonRadioGroup, IonText, TranslatePipe, LocalizedPipe],
  template: `
    <h1 tabindex="-1">{{ ballot().question | localized: language() : fallback() }}</h1>
    <p class="help">{{ 'voter.ballot.help' | translate }}</p>
    <ion-radio-group [value]="choice()" (ionChange)="choice.set($event.detail.value); missing.set(false)"
                     [attr.aria-describedby]="missing() ? 'ballot-error' : null">
      <ion-list lines="none">
        @for (option of ballot().options; track option.id) {
          <ion-item class="option" [class.selected]="choice() === option.id">
            <ion-radio [value]="option.id" justify="space-between">
              {{ option.text | localized: language() : fallback() }}
            </ion-radio>
          </ion-item>
        }
        @if (ballot().blankVoteEnabled) {
          <ion-item class="option blank" [class.selected]="choice() === 'blank'">
            <ion-radio value="blank" justify="space-between">
              <ion-label>
                {{ 'voter.ballot.blank' | translate }}
                <p>{{ 'voter.ballot.blankHelp' | translate }}</p>
              </ion-label>
            </ion-radio>
          </ion-item>
        }
      </ion-list>
    </ion-radio-group>
    @if (missing()) {
      <ion-text color="danger"><p id="ballot-error" class="error" role="alert">{{ 'voter.ballot.missing' | translate }}</p></ion-text>
    }
    <ion-button expand="block" color="secondary" (click)="next()">{{ 'voter.ballot.continue' | translate }}</ion-button>
  `,
  styles: `
    .help { color: var(--ion-color-medium); }
    ion-list { background: transparent; padding: 0; }
    .option {
      --min-height: 64px; --border-radius: var(--vv-radius-card); --background: var(--ion-color-light, #fff);
      border: 1px solid var(--vv-border-color); border-radius: var(--vv-radius-card); margin-bottom: 12px;
      font-size: 1.0625rem;
    }
    .option.selected { border: 2px solid var(--ion-color-primary); --background: rgba(var(--ion-color-primary-rgb), 0.06); }
    .blank p { font-size: 0.875rem; color: var(--ion-color-medium); margin: 2px 0 0; white-space: normal; }
    .error { margin: 0 0 0.75rem; }
    ion-button { --min-height: 52px; margin-top: 0.5rem; }
  `,
})
export class BallotStepComponent {
  readonly ballot = input.required<Ballot>();
  readonly language = input.required<string>();
  readonly fallback = input('es');
  readonly choice = model<BallotChoice>(null);
  readonly continue = output<void>();
  protected readonly missing = signal(false);

  protected next(): void {
    if (this.choice() === null) {
      this.missing.set(true);
      return;
    }
    this.continue.emit();
  }
}
