import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonButton, IonContent, IonHeader, IonIcon, IonToolbar } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../core/auth/auth.service';
import { BrandLogoComponent } from '../shared/brand-logo/brand-logo.component';
import { ErrorStateComponent } from '../shared/error-state/error-state.component';

/**
 * C5 (404). SR12: brand header and a way out. With a management session started in this tab, a button to the console;
 * without one (it may be a voter) no button, only the hint to use the personal link of the invitation email and a discreet
 * text link to the console for managers.
 * Keycloak is not initialized here on purpose: an unknown address must not send anyone to the management login.
 */
@Component({
  selector: 'app-not-found',
  imports: [IonHeader, IonToolbar, IonContent, IonButton, IonIcon, RouterLink, TranslatePipe, BrandLogoComponent, ErrorStateComponent],
  template: `
    <ion-header>
      <ion-toolbar color="tertiary"><app-brand-logo class="logo" /></ion-toolbar>
    </ion-header>
    <ion-content>
      @if (auth.authenticated()) {
        <app-error-state titleKey="errors.notFound.title" messageKey="errors.notFound.message" icon="help-circle-outline">
          <ion-button color="primary" routerLink="/admin" class="home">
            <ion-icon slot="start" name="home-outline" aria-hidden="true"></ion-icon>{{ 'common.goHome' | translate }}
          </ion-button>
        </app-error-state>
      } @else {
        <app-error-state titleKey="errors.notFound.title" messageKey="errors.notFound.voterMessage" icon="help-circle-outline">
          <a class="console" routerLink="/admin">{{ 'errors.notFound.consoleLink' | translate }}</a>
        </app-error-state>
      }
    </ion-content>
  `,
  styles: `
    .logo { padding: 0 16px; min-height: 56px; }
    .home { margin-top: 8px; }
    .console { display: inline-flex; align-items: center; min-height: 44px; margin-top: 4px; color: var(--ion-color-primary); font-size: 0.9375rem; }
  `,
})
export class NotFoundPage {
  protected readonly auth = inject(AuthService);
}
