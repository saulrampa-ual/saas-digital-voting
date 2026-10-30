import { Component, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonButton, IonCard, IonCardContent, IonIcon, IonText } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

/**
 * B4: vote recorded and receipt code (RF-10). The code is shown only now: it is not stored anywhere in the
 * browser. «Copiar» answers «Copiado». The receipt proves that the vote is in the ballot box, not what it says.
 */
@Component({
  selector: 'app-receipt-step',
  imports: [RouterLink, IonButton, IonCard, IonCardContent, IonIcon, IonText, TranslatePipe],
  template: `
    <div class="done">
      <ion-icon name="checkmark-circle-outline" aria-hidden="true"></ion-icon>
      <h1 tabindex="-1">{{ 'voter.receipt.title' | translate }}</h1>
      <p>{{ 'voter.receipt.message' | translate }}</p>
    </div>
    <ion-card>
      <ion-card-content>
        <p class="caption" id="receipt-label">{{ 'voter.receipt.codeLabel' | translate }}</p>
        <p class="code mono" aria-labelledby="receipt-label">{{ receiptCode() }}</p>
        <ion-button expand="block" fill="outline" (click)="copy()">{{ 'voter.receipt.copy' | translate }}</ion-button>
        <p class="copied" aria-live="polite">{{ copied() ? ('voter.receipt.copied' | translate) : '' }}</p>
      </ion-card-content>
    </ion-card>
    <ion-text color="medium"><p class="help">{{ 'voter.receipt.keep' | translate }}</p></ion-text>
    <ion-button expand="block" color="primary" [routerLink]="['/recibo', processId()]"
                [state]="{ receiptCode: receiptCode() }">
      {{ 'voter.receipt.verify' | translate }}
    </ion-button>
  `,
  styles: `
    .done { text-align: center; }
    .done ion-icon { font-size: 3.5rem; color: var(--ion-color-success); }
    .caption { font-size: 0.875rem; color: var(--ion-color-medium); margin: 0; }
    .code { font-size: 1.3125rem; word-break: break-all; line-height: 1.5; margin: 0.5rem 0 1rem;
      color: var(--ion-text-color); letter-spacing: 0.02em; }
    .copied { min-height: 1.25rem; margin: 0.5rem 0 0; text-align: center; color: var(--ion-color-success); }
    .help { font-size: 0.875rem; }
    ion-button { --min-height: 52px; }
  `,
})
export class ReceiptStepComponent {
  readonly receiptCode = input.required<string>();
  readonly processId = input.required<number>();
  protected readonly copied = signal(false);

  protected async copy(): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.receiptCode());
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 3000);
    } catch {
      this.copied.set(false);
    }
  }
}
