import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { addIcons } from 'ionicons';
import * as allIcons from 'ionicons/icons';
import { AuthService, ManagementRole } from '../app/core/auth/auth.service';
import { LanguageService } from '../app/core/i18n/language.service';

// Pages render with ion-icon: register the icons as the app does, so Ionic does not try to fetch SVG files
addIcons(allIcons);

/** Test double of the management session: the roles decide what `hasAnyRole` answers. Synthetic data only. */
export function fakeAuth(roles: ManagementRole[], organizationId: string | null = 'UAL') {
  return {
    hasAnyRole: (allowed: readonly ManagementRole[]) => allowed.some((role) => roles.includes(role)),
    authenticated: signal(roles.length > 0),
    roles: signal(roles),
    organizationId: signal(organizationId),
    displayName: signal('Persona de prueba'),
    userId: signal('user-1'),
  };
}

/** Router, translations (texts are their keys), language «es» and, optionally, a session with the given roles. */
export function adminProviders(roles: ManagementRole[] = ['ADMIN']) {
  return [
    provideRouter([]),
    provideTranslateService(),
    { provide: LanguageService, useValue: { current: signal('es') } },
    { provide: AuthService, useValue: fakeAuth(roles) },
  ];
}

/** Turns off the browser features that jsdom lacks (object URLs, link clicks, clipboard) for download tests. */
export function stubDownloads(clipboard: 'ok' | 'denied' = 'ok') {
  Object.assign(URL, { createObjectURL: () => 'blob:test', revokeObjectURL: () => undefined });
  HTMLAnchorElement.prototype.click = () => undefined;
  Object.defineProperty(navigator, 'clipboard', {
    configurable: true,
    value: { writeText: () => (clipboard === 'ok' ? Promise.resolve() : Promise.reject(new Error('denied'))) },
  });
}
