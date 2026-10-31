import { Component, inject } from '@angular/core';
import { IonSegment, IonSegmentButton, IonLabel } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../core/i18n/language.service';
import { AppLanguage } from '../../core/config/app-config';

/**
 * ES/EN selector of the headers (RNF-5). Touch targets of at least 44 px (prototype review).
 * Accessible name = visible code + hidden language name ("ES Español"): no aria-label on the buttons, so the
 * visible text stays inside the name (WCAG 2.5.3) and the tablist only has tab children (DEF-01, axe).
 */
@Component({
  selector: 'app-language-switcher',
  imports: [IonSegment, IonSegmentButton, IonLabel, TranslatePipe],
  template: `
    <ion-segment
      [value]="language.current()"
      (ionChange)="change($event.detail.value)"
      [attr.aria-label]="'language.label' | translate"
    >
      <ion-segment-button value="es">
        <ion-label>ES<span class="sr-only"> {{ 'language.es' | translate }}</span></ion-label>
      </ion-segment-button>
      <ion-segment-button value="en">
        <ion-label>EN<span class="sr-only"> {{ 'language.en' | translate }}</span></ion-label>
      </ion-segment-button>
    </ion-segment>
  `,
  styles: `
    ion-segment { width: 112px; }
    ion-segment-button { min-height: 44px; min-width: 44px; }
    .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
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
