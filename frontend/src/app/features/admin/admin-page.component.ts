import { Component, computed, inject, input } from '@angular/core';
import { IonHeader, IonToolbar, IonTitle, IonButtons, IonMenuButton, IonContent } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageSwitcherComponent } from '../../shared/language-switcher/language-switcher.component';
import { AuthService } from '../../core/auth/auth.service';

/**
 * Common header of the console pages: menu button (mobile), title, session context (organization, role and user,
 * from 768 px; it makes the management header clearly different from the voter one) and language switcher.
 */
@Component({
  selector: 'app-admin-page',
  imports: [IonHeader, IonToolbar, IonTitle, IonButtons, IonMenuButton, IonContent, TranslatePipe, LanguageSwitcherComponent],
  template: `
    <ion-header>
      <ion-toolbar color="primary">
        <ion-buttons slot="start"><ion-menu-button [attr.aria-label]="'admin.menu.open' | translate" /></ion-buttons>
        <ion-title><h1 class="title">{{ titleKey() | translate }}</h1></ion-title>
        <ion-buttons slot="end">
          <span class="session ion-hide-md-down">
            <span>{{ auth.displayName() }}</span>
            <span class="context">
              @for (key of roleKeys(); track key) {<span>{{ key | translate }}{{ $last ? '' : ', ' }}</span>}
              @if (auth.organizationId(); as organization) { · {{ organization }} }
            </span>
          </span>
          <app-language-switcher />
        </ion-buttons>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <div class="page"><ng-content /></div>
    </ion-content>
  `,
  styles: `
    .title { font-size: 1.125rem; margin: 0; }
    .page { max-width: 72rem; margin: 0 auto; }
    .session { display: flex; flex-direction: column; align-items: flex-end; margin-inline-end: 12px; line-height: 1.25; }
    .context { font-size: 0.875rem; opacity: 0.9; }
  `,
})
export class AdminPageComponent {
  protected readonly auth = inject(AuthService);
  protected readonly roleKeys = computed(() => this.auth.roles().map((role) => `roles.${role}`));
  readonly titleKey = input.required<string>();
}
