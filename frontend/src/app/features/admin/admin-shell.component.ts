import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { IonIcon, IonRouterOutlet } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService, ManagementRole } from '../../core/auth/auth.service';
import { AdminNavService } from './admin-nav.service';

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
  {
    path: '/admin/procesos', labelKey: 'admin.menu.processes', icon: 'calendar-outline', roles: ['ADMIN', 'AUDITOR'],
    needsOrganization: true,
  },
  {
    path: '/admin/participacion', labelKey: 'admin.menu.participation', icon: 'bar-chart-outline', roles: ['ADMIN'],
    needsOrganization: true,
  },
  {
    path: '/admin/resultados', labelKey: 'admin.menu.results', icon: 'checkbox-outline', roles: ['ADMIN', 'AUDITOR'],
    needsOrganization: true,
  },
  {
    path: '/admin/auditoria', labelKey: 'admin.menu.audit', icon: 'shield-checkmark-outline', roles: ['AUDITOR'],
    needsOrganization: true,
  },
  {
    path: '/admin/trazabilidad', labelKey: 'admin.menu.traceability', icon: 'list-outline', roles: ['ADMIN', 'AUDITOR'],
    needsOrganization: true,
  },
  {
    path: '/admin/privacidad', labelKey: 'admin.menu.privacy', icon: 'lock-closed-outline', roles: ['ADMIN'],
    needsOrganization: true,
  },
  {
    path: '/admin/claves-api', labelKey: 'admin.menu.apiKeys', icon: 'key-outline', roles: ['ADMIN'],
    needsOrganization: true,
  },
];

/**
 * Management console layout, the same for every role: a narrow rail with the section icons is always visible. The
 * hamburger button at its top expands it to show the section names and only that same button collapses it again
 * (from 992 px the content moves aside; below, the rail opens over it). The logo is in the centre of the page header
 * and the user name too, so the rail footer only keeps the logout action.
 */
@Component({
  selector: 'app-admin-shell',
  imports: [RouterLink, RouterLinkActive, TranslatePipe, IonIcon, IonRouterOutlet],
  host: { '[class.expanded]': 'nav.expanded()' },
  template: `
    <nav id="admin-nav" class="rail" [class.expanded]="nav.expanded()" [attr.aria-label]="'admin.menu.label' | translate">
      <div class="top">
        <button
          type="button"
          class="toggle"
          [attr.aria-label]="(nav.expanded() ? 'admin.menu.close' : 'admin.menu.open') | translate"
          [attr.aria-expanded]="nav.expanded()"
          aria-controls="admin-nav-entries"
          (click)="nav.toggle()"
        >
          <ion-icon name="menu-outline" aria-hidden="true"></ion-icon>
        </button>
      </div>
      <ul class="entries" id="admin-nav-entries">
        @for (entry of entries(); track entry.path) {
          <li>
            <a
              class="entry"
              [routerLink]="entry.path"
              routerLinkActive="selected"
              ariaCurrentWhenActive="page"
              [attr.title]="nav.expanded() ? null : (entry.labelKey | translate)"
            >
              <ion-icon [name]="entry.icon" aria-hidden="true"></ion-icon>
              <span class="label">{{ entry.labelKey | translate }}</span>
            </a>
          </li>
        }
      </ul>
      <button
        type="button"
        class="entry logout"
        [attr.title]="nav.expanded() ? null : ('admin.logout' | translate)"
        (click)="auth.logout()"
      >
        <ion-icon name="log-out-outline" aria-hidden="true"></ion-icon>
        <span class="label">{{ 'admin.logout' | translate }}</span>
      </button>
    </nav>
    <ion-router-outlet id="admin-content" class="outlet"></ion-router-outlet>
  `,
  styles: `
    :host { --rail-width: 64px; --rail-expanded-width: 16rem; }
    .rail {
      position: absolute; inset: 0 auto 0 0; z-index: 20; width: var(--rail-width);
      display: flex; flex-direction: column; overflow: hidden;
      background: var(--ion-background-color); border-inline-end: 1px solid rgba(var(--ion-text-color-rgb), 0.12);
      transition: width 0.2s ease, box-shadow 0.2s ease;
    }
    .rail.expanded { width: min(var(--rail-expanded-width), 85vw); box-shadow: 4px 0 16px rgba(0, 0, 0, 0.18); }
    .top { display: flex; align-items: center; flex: none; min-height: 56px; background: var(--ion-color-tertiary); }
    .toggle {
      display: grid; place-items: center; width: var(--rail-width); height: 56px; flex: none;
      border: 0; background: none; cursor: pointer; color: var(--ion-color-tertiary-contrast);
    }
    .toggle ion-icon { font-size: 28px; }
    .toggle:hover { background: rgba(255, 255, 255, 0.08); }
    .toggle:focus-visible { outline: 3px solid var(--ion-color-tertiary-contrast); outline-offset: -5px; }
    .entries { list-style: none; margin: 0; padding: 8px 0; flex: 1; overflow-y: auto; overflow-x: hidden; }
    .entry {
      display: flex; align-items: center; gap: 16px; width: 100%; min-height: 48px; padding: 0 20px;
      border: 0; background: none; font: inherit; font-size: 1rem; text-align: start; cursor: pointer;
      color: var(--ion-text-color); text-decoration: none; white-space: nowrap;
    }
    .entry:hover { background: rgba(var(--ion-color-primary-rgb), 0.06); }
    .entry:focus-visible { outline: 3px solid var(--ion-color-primary); outline-offset: -3px; }
    .entry.selected { background: rgba(var(--ion-color-primary-rgb), 0.1); font-weight: 600; color: var(--ion-color-primary); }
    .entry ion-icon { flex: none; font-size: 24px; }
    .logout { flex: none; border-top: 1px solid rgba(var(--ion-text-color-rgb), 0.12); }
    /* Collapsed: the names stay in the accessibility tree (accessible name of each link) but are not shown. */
    .rail:not(.expanded) .label {
      position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap;
    }
    /* Without a split pane the outlet must fill the space next to the rail, otherwise it collapses to zero height. */
    .outlet { position: absolute; inset: 0 0 0 var(--rail-width); transition: left 0.2s ease; }
    @media (min-width: 992px) {
      :host(.expanded) .outlet { left: var(--rail-expanded-width); }
      .rail.expanded { box-shadow: none; }
    }
  `,
})
export class AdminShellComponent {
  protected readonly auth = inject(AuthService);
  protected readonly nav = inject(AdminNavService);
  protected readonly entries = computed(() =>
    MENU.filter((entry) => this.auth.hasAnyRole(entry.roles) && (!entry.needsOrganization || this.auth.organizationId())),
  );
}
