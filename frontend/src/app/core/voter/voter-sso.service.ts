import { Injectable, inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { APP_CONFIG } from '../config/app-config';
import { AccessGranted } from './voter-api.service';

const STORAGE_KEY = 'vv.voterSso';
const MAX_AGE_MS = 10 * 60 * 1000;

interface PendingLogin {
  linkToken: string;
  verifier: string;
  state: string;
  nonce: string;
  purpose: 'vote' | 'results';
  startedAt: number;
}

/** What the callback hands to the voter flow, in memory only (never in the URL, storage or history). */
export type SsoOutcome =
  | { kind: 'granted'; granted: AccessGranted; purpose: 'vote' | 'results' }
  | { kind: 'error'; status: number; problem: Record<string, unknown>; purpose: 'vote' | 'results' };

/**
 * RF-05b (D-48): sign-in of the voter with the institutional account, OIDC Authorization Code + PKCE against
 * Keycloak (which brokers the identity provider of each organization). The voter-access service, not the browser,
 * decides whether the account is the person of the link. Across the redirect only the personal link, the PKCE verifier,
 * the state and the nonce are kept, in sessionStorage of this tab, and removed on return; the personal link never
 * travels to Keycloak (fixed redirect URI).
 */
@Injectable({ providedIn: 'root' })
export class VoterSsoService {
  private readonly document = inject(DOCUMENT);
  private outcome: SsoOutcome | null = null;

  private get redirectUri(): string {
    return `${this.document.location.origin}/votar/sso`;
  }

  private get realmUrl(): string {
    return `${APP_CONFIG.keycloak.url}/realms/${APP_CONFIG.keycloak.realm}/protocol/openid-connect`;
  }

  /** Leaves the app for the login page of the institutional account. */
  async start(linkToken: string, purpose: 'vote' | 'results', language: string): Promise<void> {
    const pending: PendingLogin = {
      linkToken, purpose, verifier: random(48), state: random(24), nonce: random(24), startedAt: Date.now(),
    };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(pending));
    const challenge = await sha256Base64Url(pending.verifier);
    const params = new URLSearchParams({
      client_id: APP_CONFIG.keycloak.voterClientId, response_type: 'code', scope: 'openid email',
      redirect_uri: this.redirectUri, state: pending.state, nonce: pending.nonce, code_challenge: challenge,
      code_challenge_method: 'S256', prompt: 'login', ui_locales: language, response_mode: 'query',
    });
    this.document.location.assign(`${this.realmUrl}/auth?${params.toString()}`);
  }

  /** On the callback: checks the state, exchanges the code and returns the personal link and the ID token. */
  async complete(query: URLSearchParams): Promise<{ linkToken: string; idToken: string; purpose: 'vote' | 'results' }> {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    sessionStorage.removeItem(STORAGE_KEY);
    const pending = raw ? (JSON.parse(raw) as PendingLogin) : null;
    if (!pending || Date.now() - pending.startedAt > MAX_AGE_MS) {
      throw new SsoError('expired', null);
    }
    if (query.get('error') || !query.get('code') || query.get('state') !== pending.state) {
      throw new SsoError(query.get('error') === 'access_denied' ? 'cancelled' : 'failed', pending);
    }
    const response = await fetch(`${this.realmUrl}/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'authorization_code', client_id: APP_CONFIG.keycloak.voterClientId, code: query.get('code')!,
        redirect_uri: this.redirectUri, code_verifier: pending.verifier,
      }),
    });
    const tokens = response.ok ? ((await response.json()) as { id_token?: string }) : {};
    if (!tokens.id_token || claim(tokens.id_token, 'nonce') !== pending.nonce) {
      throw new SsoError('failed', pending);
    }
    return { linkToken: pending.linkToken, idToken: tokens.id_token, purpose: pending.purpose };
  }

  handOff(outcome: SsoOutcome): void {
    this.outcome = outcome;
  }

  /** Read once by the voter flow. */
  take(): SsoOutcome | null {
    const outcome = this.outcome;
    this.outcome = null;
    return outcome;
  }
}

export class SsoError extends Error {
  constructor(readonly reason: 'expired' | 'cancelled' | 'failed', readonly pending: PendingLogin | null) {
    super(reason);
  }
}

function random(bytes: number): string {
  const values = new Uint8Array(bytes);
  crypto.getRandomValues(values);
  return base64Url(values);
}

async function sha256Base64Url(text: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text));
  return base64Url(new Uint8Array(digest));
}

function base64Url(bytes: Uint8Array): string {
  let binary = '';
  bytes.forEach((byte) => (binary += String.fromCharCode(byte)));
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function claim(jwt: string, name: string): unknown {
  try {
    const payload = jwt.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return (JSON.parse(atob(payload.padEnd(payload.length + ((4 - (payload.length % 4)) % 4), '='))) as Record<string, unknown>)[name];
  } catch {
    return undefined;
  }
}
