import { Component, inject } from '@angular/core';
import { IonApp, IonRouterOutlet } from '@ionic/angular';
import { addIcons } from 'ionicons';
import {
  addOutline, alertCircleOutline, barChartOutline, businessOutline, calendarOutline, checkboxOutline, checkmarkCircleOutline, checkmarkOutline,
  closeOutline, closeCircleOutline, cloudOfflineOutline, cloudUploadOutline, copyOutline, createOutline,
  documentTextOutline, listOutline, downloadOutline, helpCircleOutline, homeOutline, informationCircleOutline,
  linkOutline, lockClosedOutline, peopleOutline, refreshOutline, shieldCheckmarkOutline, timeOutline,
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
      addOutline, alertCircleOutline, barChartOutline, businessOutline, calendarOutline, checkboxOutline, checkmarkCircleOutline, checkmarkOutline,
      closeOutline, closeCircleOutline, cloudOfflineOutline, cloudUploadOutline, copyOutline, createOutline,
  documentTextOutline, listOutline, downloadOutline, helpCircleOutline, homeOutline, informationCircleOutline,
      linkOutline, lockClosedOutline, peopleOutline, refreshOutline, shieldCheckmarkOutline, timeOutline,
    });
    inject(LanguageService).init();
    inject(FocusOnNavigationService).init();
  }
}
