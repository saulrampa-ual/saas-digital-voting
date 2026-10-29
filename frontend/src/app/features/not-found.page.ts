import { Component } from '@angular/core';
import { IonContent } from '@ionic/angular';
import { ErrorStateComponent } from '../shared/error-state/error-state.component';

/** C5 (404). */
@Component({
  selector: 'app-not-found',
  imports: [IonContent, ErrorStateComponent],
  template: `
    <ion-content>
      <app-error-state titleKey="errors.notFound.title" messageKey="errors.notFound.message" icon="help-circle-outline" />
    </ion-content>
  `,
})
export class NotFoundPage {}
