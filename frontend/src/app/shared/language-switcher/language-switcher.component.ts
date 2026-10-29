import { Component, inject } from '@angular/core';
import { IonSegment, IonSegmentButton, IonLabel } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../core/i18n/language.service';
import { AppLanguage } from '../../core/config/app-config';

/** ES/EN selector of the headers (RNF-5). Touch targets of at least 44 px (prototype review). */
@Component({
  selector: 'app-language-switcher',
  imports: [IonSegment, IonSegmentButton, IonLabel, TranslatePipe],
  template: `
    <ion-segment
      [value]="language.current()"
      (ionChange)="change($event.detail.value)"
      [attr.aria-label]="'language.label' | translate"
    >
      <ion-segment-button value="es" [attr.aria-label]="'language.es' | translate">
        <ion-label>ES</ion-label>
      </ion-segment-button>
      <ion-segment-button value="en" [attr.aria-label]="'language.en' | translate">
        <ion-label>EN</ion-label>
      </ion-segment-button>
    </ion-segment>
  `,
  styles: `
    ion-segment { width: 112px; }
    ion-segment-button { min-height: 44px; min-width: 44px; }
  `,
})
export class LanguageSwitcherComponent {
  protected readonly language = inject(LanguageService);

  protected change(value: unknown): void {
    if (value === 'es' || value === 'en') {
      this.language.use(value as AppLanguage);
    }
  }
}
