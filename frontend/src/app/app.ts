import { Component, inject } from '@angular/core';
import { IonApp, IonRouterOutlet } from '@ionic/angular';
import { addIcons } from 'ionicons';
import {
  addOutline, alertCircleOutline, businessOutline, calendarOutline, checkmarkCircleOutline, checkmarkOutline,
  closeOutline, closeCircleOutline, cloudOfflineOutline, cloudUploadOutline, createOutline, helpCircleOutline, homeOutline, informationCircleOutline,
  linkOutline, lockClosedOutline, peopleOutline, timeOutline,
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
    addIcons({
      addOutline, alertCircleOutline, businessOutline, calendarOutline, checkmarkCircleOutline, checkmarkOutline,
      closeOutline, closeCircleOutline, cloudOfflineOutline, cloudUploadOutline, createOutline, helpCircleOutline, homeOutline, informationCircleOutline,
      linkOutline, lockClosedOutline, peopleOutline, timeOutline,
    });
    inject(LanguageService).init();
    inject(FocusOnNavigationService).init();
  }
}
