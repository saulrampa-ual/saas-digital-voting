import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { SsoError, VoterSsoService } from './voter-sso.service';

describe('VoterSsoService', () => {
  const location = { origin: 'http://localhost:4200', assign: vi.fn() };
  let service: VoterSsoService;
  let randomByte = 1;

  beforeEach(() => {
    sessionStorage.clear();
    randomByte = 1;
    const platformSubtle = globalThis.crypto.subtle;
    vi.stubGlobal('crypto', {
      getRandomValues(values: Uint8Array) {
        for (let index = 0; index < values.length; index += 1) {
          values[index] = randomByte++ % 256;
        }
        return values;
      },
      subtle: platformSubtle,
    });
    TestBed.configureTestingModule({
      providers: [VoterSsoService, { provide: DOCUMENT, useValue: { location } }],
    });
    service = TestBed.inject(VoterSsoService);
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('starts with PKCE S256, state and nonce without sending the personal link to Keycloak', async () => {
    await service.start('synthetic-personal-link', 'vote', 'es');

    const redirectUrl = new URL(location.assign.mock.calls[0][0]);
    const pending = JSON.parse(sessionStorage.getItem('vv.voterSso')!) as {
      linkToken: string; verifier: string; state: string; nonce: string; purpose: string;
    };
    const digest = await globalThis.crypto.subtle.digest('SHA-256', new TextEncoder().encode(pending.verifier));
    const expectedChallenge = btoa(String.fromCharCode(...new Uint8Array(digest)))
      .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    expect(redirectUrl.pathname).toBe('/realms/verivoto/protocol/openid-connect/auth');
    expect(redirectUrl.searchParams.get('code_challenge_method')).toBe('S256');
    expect(redirectUrl.searchParams.get('code_challenge')).toBe(expectedChallenge);
    expect(redirectUrl.searchParams.get('state')).toBe(pending.state);
    expect(redirectUrl.searchParams.get('nonce')).toBe(pending.nonce);
    expect(pending.verifier).toBeTruthy();
    expect(pending.purpose).toBe('vote');
    expect(redirectUrl.href).not.toContain(pending.linkToken);
  });

  it('rejects a callback with a different state and removes the pending login', async () => {
    await service.start('synthetic-personal-link', 'results', 'en');

    await expect(service.complete(new URLSearchParams({ code: 'synthetic-code', state: 'wrong-state' })))
      .rejects.toMatchObject({ reason: 'failed' });
    expect(sessionStorage.getItem('vv.voterSso')).toBeNull();
  });

  it('exchanges the code with the verifier and checks the returned nonce', async () => {
    await service.start('synthetic-personal-link', 'vote', 'es');
    const pending = JSON.parse(sessionStorage.getItem('vv.voterSso')!) as {
      linkToken: string; verifier: string; state: string; nonce: string;
    };
    const jwtPayload = btoa(JSON.stringify({ nonce: pending.nonce }))
      .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ id_token: `header.${jwtPayload}.signature` }),
    });
    vi.stubGlobal('fetch', fetchMock);

    await expect(service.complete(new URLSearchParams({ code: 'synthetic-code', state: pending.state })))
      .resolves.toEqual({ linkToken: 'synthetic-personal-link', idToken: `header.${jwtPayload}.signature`,
        purpose: 'vote' });

    const [, request] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    const body = new URLSearchParams(request.body as string);
    expect(body.get('code_verifier')).toBe(pending.verifier);
    expect(body.has('linkToken')).toBe(false);
    expect(sessionStorage.getItem('vv.voterSso')).toBeNull();
  });

  it('returns the handoff outcome only once', () => {
    const outcome = { kind: 'error' as const, status: 503, problem: {}, purpose: 'vote' as const };
    service.handOff(outcome);

    expect(service.take()).toBe(outcome);
    expect(service.take()).toBeNull();
  });
});
