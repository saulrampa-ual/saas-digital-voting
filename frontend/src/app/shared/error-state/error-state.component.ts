import { Component, input } from '@angular/core';
import { IonIcon, IonText } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

/** Full-page error or information state (screens C2–C5, V3–V7). The message is announced to screen readers. */
@Component({
  selector: 'app-error-state',
  imports: [IonIcon, IonText, TranslatePipe],
  template: `
    <section class="state" role="alert">
      <ion-icon [name]="icon()" aria-hidden="true"></ion-icon>
      <h1 tabindex="-1">{{ titleKey() | translate }}</h1>
      <ion-text color="medium"><p>{{ messageKey() | translate: messageParams() }}</p></ion-text>
      <ng-content />
    </section>
  `,
  styles: `
    .state { max-width: 32rem; margin: 3rem auto; padding: 0 1rem; text-align: center; }
    ion-icon { font-size: 3rem; color: var(--ion-color-primary); }
    h1 { font-size: 1.5rem; margin: 1rem 0 0.5rem; }
    p { font-size: 1rem; }
  `,
})
export class ErrorStateComponent {
  readonly titleKey = input.required<string>();
  readonly messageKey = input.required<string>();
  readonly icon = input('alert-circle-outline');
  readonly messageParams = input<Record<string, unknown>>({});
}
