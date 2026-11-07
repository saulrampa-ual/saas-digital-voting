import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonButton, IonIcon } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { ErrorStateComponent } from '../../../shared/error-state/error-state.component';
import { AdminPageComponent } from '../admin-page.component';

/** SR12: same way out as the 404 page, back to the console home. */
@Component({
  selector: 'app-forbidden',
  imports: [AdminPageComponent, ErrorStateComponent, IonButton, IonIcon, RouterLink, TranslatePipe],
  template: `
    <app-admin-page titleKey="admin.forbidden.title">
      <app-error-state titleKey="admin.forbidden.title" messageKey="admin.forbidden.message" icon="lock-closed-outline">
        <ion-button color="primary" routerLink="/admin/inicio" class="home">
          <ion-icon slot="start" name="home-outline" aria-hidden="true"></ion-icon>{{ 'common.goHome' | translate }}
        </ion-button>
      </app-error-state>
    </app-admin-page>
  `,
  styles: `.home { margin-top: 8px; }`,
})
export class ForbiddenPage {}
