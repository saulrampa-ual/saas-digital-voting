import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import Keycloak from 'keycloak-js';

/** The warning appears this long before the management session expires (D1, IMP-15, WCAG 2.2.1). */
export const SESSION_WARNING_SECONDS = 120;

export type SessionState = 'active' | 'warning' | 'limit' | 'expired';

/**
 * Watches the management session (Keycloak SSO session, idle 30 min and 8 h at most) through the expiry of the refresh
 * token, which Keycloak moves forward on every refresh. Two minutes before it ends the console warns; «Seguir aquí»
 * refreshes the token, which extends the idle time. When the maximum lifespan is reached the session cannot be
 * extended (`limit`). Nothing here sends or stores data: unsaved process drafts stay in this browser (D-59).
 */
@Injectable({ providedIn: 'root' })
export class SessionTimeoutService {
  private readonly keycloak = inject(Keycloak);
  private timer: ReturnType<typeof setInterval> | null = null;

  /** Seconds until the session ends, or null when unknown (not signed in). */
  readonly secondsLeft = signal<number | null>(null);
  /** «Seguir aquí» was pressed and the session could not be extended any further. */
  private readonly atLimit = signal(false);
  readonly extending = signal(false);

  readonly state = computed<SessionState>(() => {
    const left = this.secondsLeft();
    if (left === null || left > SESSION_WARNING_SECONDS) return 'active';
    if (left <= 0) return 'expired';
    return this.atLimit() ? 'limit' : 'warning';
  });

  constructor() {
    inject(DestroyRef).onDestroy(() => this.stop());
  }

  /** Starts checking every second (idempotent). */
  start(): void {
    this.check();
    this.timer ??= setInterval(() => this.check(), 1000);
  }

  stop(): void {
    if (this.timer) clearInterval(this.timer);
    this.timer = null;
  }

  /** Recomputes the time left; `nowMs` only for tests. */
  check(nowMs = Date.now()): void {
    const exp = this.keycloak.refreshTokenParsed?.exp;
    if (!exp) {
      this.secondsLeft.set(null);
      return;
    }
    // timeSkew = server time − browser time (s), as in keycloak-js isTokenExpired()
    const left = Math.floor(exp - nowMs / 1000 + (this.keycloak.timeSkew ?? 0));
    if (left > SESSION_WARNING_SECONDS) this.atLimit.set(false);
    this.secondsLeft.set(left);
  }

  /** «Seguir aquí»: forces a token refresh, which resets the idle time of the session. */
  async extend(): Promise<void> {
    this.extending.set(true);
    try {
      await this.keycloak.updateToken(-1);
    } catch {
      // The session already ended on the server: the next check shows «expired» once the time is up.
    } finally {
      this.extending.set(false);
    }
    this.check();
    const left = this.secondsLeft();
    this.atLimit.set(left !== null && left > 0 && left <= SESSION_WARNING_SECONDS);
  }

  /** «Volver a entrar» after expiry: back to the login page and then to the same screen. */
  login(): Promise<void> {
    return this.keycloak.login({ redirectUri: window.location.href });
  }
}
