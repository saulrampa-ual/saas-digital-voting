import { Component, input } from '@angular/core';
import { IonIcon, IonSpinner } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

export type VerificationState = 'checking' | 'ok' | 'mismatch' | 'error';

/** «Resultado verificado» (F1, V8): the browser recomputed the result from the published votes (RF-13, D-37). */
@Component({
  selector: 'app-result-verification',
  imports: [IonIcon, IonSpinner, TranslatePipe],
  template: `
    <section class="verification" [class]="'verification ' + state()" aria-live="polite">
      @switch (state()) {
        @case ('checking') {
          <h3><ion-spinner name="crescent" aria-hidden="true"></ion-spinner> {{ 'results.verify.checking' | translate }}</h3>
        }
        @case ('ok') {
          <h3><ion-icon name="shield-checkmark-outline" aria-hidden="true"></ion-icon> {{ 'results.verify.okTitle' | translate }}</h3>
          <p>{{ 'results.verify.okText' | translate }}</p>
        }
        @case ('mismatch') {
          <h3><ion-icon name="alert-circle-outline" aria-hidden="true"></ion-icon> {{ 'results.verify.badTitle' | translate }}</h3>
          <p>{{ 'results.verify.badText' | translate }}</p>
        }
        @default {
          <h3><ion-icon name="alert-circle-outline" aria-hidden="true"></ion-icon> {{ 'results.verify.errorTitle' | translate }}</h3>
          <p>{{ 'results.verify.errorText' | translate }}</p>
        }
      }
      <dl class="hashes">
        <dt>{{ 'results.resultHash' | translate }}</dt><dd class="mono">{{ resultHash() }}</dd>
        <dt>{{ 'results.finalHash' | translate }}</dt><dd class="mono">{{ finalHash() }}</dd>
      </dl>
      <p class="limits">{{ 'results.verify.limits' | translate }}</p>
    </section>
  `,
  styles: `
    .verification { border: 2px solid #c9d2dd; border-radius: var(--vv-radius-card, 12px); padding: 16px 20px; background: #fff; }
    .verification.ok { border-color: var(--ion-color-success); }
    .verification.mismatch, .verification.error { border-color: var(--ion-color-danger); }
    h3 { display: flex; gap: 8px; align-items: center; margin: 0 0 4px; font-size: 1.125rem; }
    .ok h3 { color: var(--ion-color-success); }
    .mismatch h3, .error h3 { color: var(--ion-color-danger); }
    p { margin: 0 0 8px; }
    .hashes { margin: 8px 0 0; display: grid; gap: 2px; }
    dt { font-size: 0.875rem; color: #4a5868; margin-top: 6px; }
    dd { margin: 0; font-size: 0.8125rem; overflow-wrap: anywhere; }
    .limits { font-size: 0.8125rem; color: var(--ion-color-medium); margin-top: 10px; }
  `,
})
export class ResultVerificationComponent {
  readonly state = input.required<VerificationState>();
  readonly resultHash = input.required<string>();
  readonly finalHash = input.required<string>();
}
