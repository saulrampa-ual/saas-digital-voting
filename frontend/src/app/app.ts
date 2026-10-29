import { Component, inject } from '@angular/core';
import { IonApp, IonRouterOutlet } from '@ionic/angular';
import { addIcons } from 'ionicons';
import {
  alertCircleOutline, businessOutline, helpCircleOutline, homeOutline, lockClosedOutline, peopleOutline,
} from 'ionicons/icons';
import { LanguageService } from './core/i18n/language.service';
import { FocusOnNavigationService } from './core/a11y/focus-on-navigation.service';

@Component({
  selector: 'app-root',
  imports: [IonApp, IonRouterOutlet],
  template: `<ion-app><ion-router-outlet /></ion-app>`,
})
export class App {
  constructor() {
    addIcons({ alertCircleOutline, businessOutline, helpCircleOutline, homeOutline, lockClosedOutline, peopleOutline });
    inject(LanguageService).init();
    inject(FocusOnNavigationService).init();
  }
}
