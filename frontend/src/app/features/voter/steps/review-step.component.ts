import { Component, input, output } from '@angular/core';
import { IonButton, IonCard, IonCardContent, IonIcon, IonSpinner, IonText } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

/**
 * B3: «Revisa tu voto» on its own screen (RNF-7). The confirm button is disabled once pressed, and there are 12 px
 * between «Cambiar mi selección» and «Confirmar y votar» to avoid accidental taps.
 */
@Component({
  selector: 'app-review-step',
  imports: [IonButton, IonCard, IonCardContent, IonIcon, IonSpinner, IonText, TranslatePipe],
  template: `
    <h1 tabindex="-1">{{ 'voter.review.title' | translate }}</h1>
    <ion-card class="choice">
      <ion-card-content>
        <p class="caption">{{ 'voter.review.yourChoice' | translate }}</p>
        <p class="selected">{{ choiceText() }}</p>
        @if (blank()) {
          <p class="blank-help">{{ 'voter.ballot.blankHelp' | translate }}</p>
        }
      </ion-card-content>
    </ion-card>
    <div class="notice" role="note">
      <ion-icon name="information-circle-outline" aria-hidden="true"></ion-icon>
      <ion-text><p>{{ 'voter.review.notYet' | translate }}</p></ion-text>
    </div>
    <div class="actions">
      <ion-button expand="block" fill="outline" [disabled]="busy()" (click)="changeSelection.emit()">
        {{ 'voter.review.change' | translate }}
      </ion-button>
      <ion-button expand="block" color="secondary" [disabled]="busy()" (click)="confirm.emit()">
        @if (busy()) {
          <ion-spinner name="crescent" aria-hidden="true"></ion-spinner>&nbsp;{{ 'voter.review.sending' | translate }}
        } @else {
          {{ 'voter.review.confirm' | translate }}
        }
      </ion-button>
    </div>
    @if (busy()) {
      <ion-text color="medium"><p class="wait" role="status">{{ 'voter.review.doNotClose' | translate }}</p></ion-text>
    }
  `,
  styles: `
    .choice { margin: 0 0 1rem; }
    .caption { font-size: 0.875rem; color: var(--ion-color-medium); margin: 0; }
    .selected { font-size: 1.375rem; font-weight: 700; color: var(--ion-text-color); margin: 0.25rem 0 0; }
    .blank-help { font-size: 0.875rem; margin: 0.5rem 0 0; }
    .notice { display: flex; gap: 8px; align-items: flex-start; background: #fff4dc; color: #7a4b00;
      border-radius: var(--vv-radius-control); padding: 12px; margin-bottom: 1rem; }
    .notice ion-icon { font-size: 1.5rem; flex: none; }
    .notice p { margin: 0; }
    .actions { display: grid; gap: 12px; }
    ion-button { --min-height: 52px; margin: 0; }
    .wait { text-align: center; }
  `,
})
export class ReviewStepComponent {
  readonly choiceText = input.required<string>();
  readonly blank = input(false);
  readonly busy = input(false);
  /** «Cambiar mi selección» (not `change`, which is also a native DOM event). */
  readonly changeSelection = output<void>();
  readonly confirm = output<void>();
}
