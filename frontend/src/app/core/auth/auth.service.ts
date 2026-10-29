import { Injectable, inject, signal, computed } from '@angular/core';
import Keycloak from 'keycloak-js';

/** Management roles carried in the `roles` claim of the Keycloak token (D-02). */
export type ManagementRole = 'SUPERADMIN' | 'ADMIN' | 'CENSUS_MANAGER' | 'AUDITOR';

const MANAGEMENT_ROLES: readonly ManagementRole[] = ['SUPERADMIN', 'ADMIN', 'CENSUS_MANAGER', 'AUDITOR'];

/**
 * Login of management users with Keycloak (Authorization Code + PKCE, RF-04).
 * Keycloak is initialized lazily, only when the management console is opened: the voter area never depends on it.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = inject(Keycloak);
  private initialization?: Promise<boolean>;

  private readonly claims = signal<Record<string, unknown> | null>(null);

  readonly authenticated = computed(() => this.claims() !== null);
  readonly roles = computed<ManagementRole[]>(() => {
    const roles = this.claims()?.['roles'];
    return Array.isArray(roles) ? MANAGEMENT_ROLES.filter((role) => roles.includes(role)) : [];
  });
  readonly organizationId = computed(() => {
    const value = this.claims()?.['organization_id'];
    return typeof value === 'string' && value.length > 0 ? value : null;
  });
  readonly displayName = computed(() => {
    const claims = this.claims();
    return (claims?.['name'] as string | undefined) ?? (claims?.['preferred_username'] as string | undefined) ?? '';
  });

  /** Initializes Keycloak once and redirects to the login page if there is no session. */
  async ensureLoggedIn(): Promise<boolean> {
    this.initialization ??= this.keycloak.init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    });
    const authenticated = await this.initialization;
    this.claims.set(authenticated ? ((this.keycloak.tokenParsed as Record<string, unknown>) ?? null) : null);
    this.keycloak.onAuthRefreshSuccess = () =>
      this.claims.set((this.keycloak.tokenParsed as Record<string, unknown>) ?? null);
    return authenticated;
  }

  hasAnyRole(allowed: readonly ManagementRole[]): boolean {
    return this.roles().some((role) => allowed.includes(role));
  }

  logout(): Promise<void> {
    return this.keycloak.logout({ redirectUri: window.location.origin + '/admin' });
  }
}
