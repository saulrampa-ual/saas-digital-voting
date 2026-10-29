import { Component, input } from '@angular/core';
import { IonHeader, IonToolbar, IonTitle, IonButtons, IonContent, IonFooter, IonText } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageSwitcherComponent } from '../../shared/language-switcher/language-switcher.component';

/**
 * Entry of the voter flow from the personal link (/votar/:linkToken). Placeholder until P4.5 (V1, B1–B5).
 * The voter area does not use Keycloak: access is the personal link plus the email code (RF-08).
 */
@Component({
  selector: 'app-voter-entry',
  imports: [IonHeader, IonToolbar, IonTitle, IonButtons, IonContent, IonFooter, IonText, TranslatePipe, LanguageSwitcherComponent],
  template: `
    <ion-header>
      <ion-toolbar color="tertiary">
        <ion-title>VeriVoto</ion-title>
        <ion-buttons slot="end"><app-language-switcher /></ion-buttons>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <main class="voter">
        <h1 tabindex="-1">{{ 'voter.welcome.title' | translate }}</h1>
        <p>{{ 'voter.welcome.pending' | translate }}</p>
      </main>
    </ion-content>
    <ion-footer class="ion-padding">
      <ion-text color="medium"><p class="secure">{{ 'voter.footer.secure' | translate }}</p></ion-text>
    </ion-footer>
  `,
  styles: `
    .voter { max-width: 36rem; margin: 0 auto; }
    h1 { font-size: 1.5rem; }
    .secure { text-align: center; margin: 0; font-size: 0.875rem; }
  `,
})
export class VoterEntryPage {
  /** Bound from the route (withComponentInputBinding). Never logged or shown. */
  readonly linkToken = input<string>();
}
