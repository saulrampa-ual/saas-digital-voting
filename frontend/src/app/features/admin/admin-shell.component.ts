import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {
  IonSplitPane, IonMenu, IonHeader, IonToolbar, IonTitle, IonContent, IonList, IonItem, IonLabel,
  IonIcon, IonMenuToggle, IonRouterOutlet, IonFooter, IonButton, IonNote,
} from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService, ManagementRole } from '../../core/auth/auth.service';

interface MenuEntry {
  readonly path: string;
  readonly labelKey: string;
  readonly icon: string;
  readonly roles: readonly ManagementRole[];
  /** Organization-scoped sections are hidden from users without organization (super-administrator). */
  readonly needsOrganization?: boolean;
}

const MENU: readonly MenuEntry[] = [
  { path: '/admin/inicio', labelKey: 'admin.menu.home', icon: 'home-outline', roles: ['SUPERADMIN', 'ADMIN', 'CENSUS_MANAGER', 'AUDITOR'] },
  { path: '/admin/organizaciones', labelKey: 'admin.menu.organizations', icon: 'business-outline', roles: ['SUPERADMIN'] },
  {
    path: '/admin/censos', labelKey: 'admin.menu.censuses', icon: 'people-outline', roles: ['ADMIN', 'CENSUS_MANAGER'],
    needsOrganization: true,
  },
];

/** Management console layout: fixed menu from 992 px, hamburger menu below (prototype guide). */
@Component({
  selector: 'app-admin-shell',
  imports: [
    RouterLink, RouterLinkActive, TranslatePipe, IonSplitPane, IonMenu, IonHeader, IonToolbar, IonTitle,
    IonContent, IonList, IonItem, IonLabel, IonIcon, IonMenuToggle, IonRouterOutlet, IonFooter, IonButton, IonNote,
  ],
  template: `
    <ion-split-pane contentId="admin-content" when="lg">
      <ion-menu contentId="admin-content" type="overlay">
        <ion-header>
          <ion-toolbar color="tertiary">
            <ion-title>VeriVoto</ion-title>
          </ion-toolbar>
        </ion-header>
        <ion-content>
          <nav [attr.aria-label]="'admin.menu.label' | translate">
            <ion-list lines="none">
              @for (entry of entries(); track entry.path) {
                <ion-menu-toggle [autoHide]="false">
                  <ion-item button [routerLink]="entry.path" routerLinkActive="selected" detail="false">
                    <ion-icon slot="start" [name]="entry.icon" aria-hidden="true"></ion-icon>
                    <ion-label>{{ entry.labelKey | translate }}</ion-label>
                  </ion-item>
                </ion-menu-toggle>
              }
            </ion-list>
          </nav>
        </ion-content>
        <ion-footer>
          <ion-toolbar>
            <ion-note class="user">{{ auth.displayName() }}</ion-note>
            <ion-button slot="end" fill="clear" (click)="auth.logout()">{{ 'admin.logout' | translate }}</ion-button>
          </ion-toolbar>
        </ion-footer>
      </ion-menu>
      <ion-router-outlet id="admin-content"></ion-router-outlet>
    </ion-split-pane>
  `,
  styles: `
    ion-item.selected { --background: rgba(var(--ion-color-primary-rgb), 0.08); font-weight: 600; }
    ion-item { --min-height: 48px; }
    .user { padding-inline-start: 16px; }
  `,
})
export class AdminShellComponent {
  protected readonly auth = inject(AuthService);
  protected readonly entries = computed(() =>
    MENU.filter((entry) => this.auth.hasAnyRole(entry.roles) && (!entry.needsOrganization || this.auth.organizationId())),
  );
}
