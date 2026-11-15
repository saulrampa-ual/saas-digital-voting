import { Injectable, inject, signal, computed } from '@angular/core';
import Keycloak from 'keycloak-js';
import { APP_CONFIG, AppLanguage } from '../config/app-config';
import { LanguageService } from '../i18n/language.service';

/** Management roles carried in the `roles` claim of the Keycloak token (D-02). */
export type ManagementRole = 'SUPERADMIN' | 'ADMIN' | 'CENSUS_MANAGER' | 'AUDITOR';

const MANAGEMENT_ROLES: readonly ManagementRole[] = ['SUPERADMIN', 'ADMIN', 'CENSUS_MANAGER', 'AUDITOR'];

/** Prefix of the unsaved A3 drafts kept in this browser (see `ProcessFormPage`, D-59). */
export const PROCESS_DRAFT_PREFIX = 'vv.processDraft:';

/** Login (`auth_time`) whose language was already taken from Keycloak, kept for this tab only. */
const LOGIN_LANGUAGE_KEY = 'vv.loginLanguage';

/** Removes every unsaved A3 draft of this browser (on logout, so the next person never sees them). */
export function clearProcessDrafts(): void {
  try {
    for (let i = localStorage.length - 1; i >= 0; i--) {
      const key = localStorage.key(i);
      if (key?.startsWith(PROCESS_DRAFT_PREFIX)) localStorage.removeItem(key);
    }
  } catch {
    // Storage unavailable (private mode, blocked): nothing was kept.
  }
}

/**
 * Login of management users with Keycloak (Authorization Code + PKCE, RF-04).
 * Keycloak is initialized lazily, only when the management console is opened: the voter area never depends on it.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = inject(Keycloak);
  private readonly language = inject(LanguageService);
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
  /** Keycloak subject of the person signed in (`sub`); used to keep per-person browser data apart. */
  readonly userId = computed(() => {
    const value = this.claims()?.['sub'];
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
      // The login page opens in the language of the console (`ui_locales`)
      locale: this.language.current(),
    });
    const authenticated = await this.initialization;
    this.claims.set(authenticated ? ((this.keycloak.tokenParsed as Record<string, unknown>) ?? null) : null);
    if (authenticated) this.adoptLoginLanguage();
    this.keycloak.onAuthRefreshSuccess = () =>
      this.claims.set((this.keycloak.tokenParsed as Record<string, unknown>) ?? null);
    return authenticated;
  }

  /**
   * The language chosen on the login page (claim `locale`) becomes the language of the console, once per login:
   * afterwards only the flags of the header change it, also across reloads.
   */
  private adoptLoginLanguage(): void {
    const claims = { ...this.keycloak.tokenParsed, ...this.keycloak.idTokenParsed } as Record<string, unknown>;
    const locale = claims['locale'];
    const login = String(claims['auth_time'] ?? claims['sid'] ?? '');
    if (typeof locale !== 'string' || !login) return;
    try {
      if (sessionStorage.getItem(LOGIN_LANGUAGE_KEY) === login) return;
      sessionStorage.setItem(LOGIN_LANGUAGE_KEY, login);
    } catch {
      return; // Storage blocked: the language of the console is left as it is.
    }
    if ((APP_CONFIG.languages as readonly string[]).includes(locale) && locale !== this.language.current()) {
      this.language.use(locale as AppLanguage);
    }
  }

  hasAnyRole(allowed: readonly ManagementRole[]): boolean {
    return this.roles().some((role) => allowed.includes(role));
  }

  logout(): Promise<void> {
    clearProcessDrafts();
    return this.keycloak.logout({ redirectUri: window.location.origin + '/admin' });
  }
}
