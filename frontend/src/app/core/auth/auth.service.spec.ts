import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { LanguageService } from '../i18n/language.service';
import { AuthService, PROCESS_DRAFT_PREFIX, clearProcessDrafts } from './auth.service';

/** RF-04: login of management users. Keycloak is replaced by a double; no real session or token is used. */
describe('AuthService', () => {
  let keycloak: {
    init: ReturnType<typeof vi.fn>; logout: ReturnType<typeof vi.fn>;
    tokenParsed?: Record<string, unknown>; idTokenParsed?: Record<string, unknown>; onAuthRefreshSuccess?: () => void;
  };
  let language: { current: ReturnType<typeof signal<string>>; use: ReturnType<typeof vi.fn> };
  let service: AuthService;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    keycloak = { init: vi.fn().mockResolvedValue(true), logout: vi.fn().mockResolvedValue(undefined) };
    language = { current: signal('es'), use: vi.fn() };
    TestBed.configureTestingModule({
      providers: [{ provide: Keycloak, useValue: keycloak }, { provide: LanguageService, useValue: language }],
    });
    service = TestBed.inject(AuthService);
  });
  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('has no session data before the login or when Keycloak reports no session', async () => {
    keycloak.init.mockResolvedValue(false);
    expect(await service.ensureLoggedIn()).toBe(false);
    expect(service.authenticated()).toBe(false);
    expect(service.roles()).toEqual([]);
    expect(service.organizationId()).toBeNull();
    expect(service.userId()).toBeNull();
    expect(service.displayName()).toBe('');
    expect(service.hasAnyRole(['ADMIN'])).toBe(false);
  });

  it('reads roles, organization, user and name from the token and initializes Keycloak only once', async () => {
    keycloak.tokenParsed = { sub: 'user-1', roles: ['ADMIN', 'offline_access'], organization_id: 'org-1', name: 'Ana Ruiz' };
    expect(await service.ensureLoggedIn()).toBe(true);
    await service.ensureLoggedIn();

    expect(keycloak.init).toHaveBeenCalledTimes(1);
    expect(keycloak.init).toHaveBeenCalledWith(expect.objectContaining({ onLoad: 'login-required', pkceMethod: 'S256', locale: 'es' }));
    expect(service.authenticated()).toBe(true);
    expect(service.roles()).toEqual(['ADMIN']);
    expect(service.organizationId()).toBe('org-1');
    expect(service.userId()).toBe('user-1');
    expect(service.displayName()).toBe('Ana Ruiz');
    expect(service.hasAnyRole(['ADMIN', 'AUDITOR'])).toBe(true);
    expect(service.hasAnyRole(['AUDITOR'])).toBe(false);
  });

  it('falls back to the username and ignores malformed claims', async () => {
    keycloak.tokenParsed = { sub: '', roles: 'ADMIN', organization_id: 7, preferred_username: 'ana' };
    await service.ensureLoggedIn();

    expect(service.displayName()).toBe('ana');
    expect(service.roles()).toEqual([]);
    expect(service.organizationId()).toBeNull();
    expect(service.userId()).toBeNull();
  });

  it('updates the claims when the token is refreshed', async () => {
    keycloak.tokenParsed = { sub: 'user-1', roles: ['AUDITOR'] };
    await service.ensureLoggedIn();
    keycloak.tokenParsed = { sub: 'user-1', roles: ['ADMIN'] };
    keycloak.onAuthRefreshSuccess?.();
    expect(service.roles()).toEqual(['ADMIN']);

    keycloak.tokenParsed = undefined;
    keycloak.onAuthRefreshSuccess?.();
    expect(service.authenticated()).toBe(false);
  });

  it('adopts the language chosen on the login page once per login', async () => {
    keycloak.tokenParsed = { sub: 'user-1', auth_time: 1000 };
    keycloak.idTokenParsed = { locale: 'en' };
    await service.ensureLoggedIn();
    await service.ensureLoggedIn();

    expect(language.use).toHaveBeenCalledTimes(1);
    expect(language.use).toHaveBeenCalledWith('en');
  });

  it('keeps the console language when the login one is missing, unsupported or the same', async () => {
    keycloak.tokenParsed = { sub: 'user-1', auth_time: 1 };
    await service.ensureLoggedIn(); // no locale
    keycloak.tokenParsed = { sub: 'user-1', locale: 'en' };
    await service.ensureLoggedIn(); // no login identifier
    keycloak.tokenParsed = { sub: 'user-1', sid: 'a', locale: 'fr' };
    await service.ensureLoggedIn(); // unsupported
    keycloak.tokenParsed = { sub: 'user-1', sid: 'b', locale: 'es' };
    await service.ensureLoggedIn(); // already the current one

    expect(language.use).not.toHaveBeenCalled();
  });

  it('keeps the console language when the browser storage is blocked', async () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('blocked'); });
    keycloak.tokenParsed = { sub: 'user-1', auth_time: 2, locale: 'en' };
    await service.ensureLoggedIn();

    expect(language.use).not.toHaveBeenCalled();
  });

  it('removes the unsaved process drafts on logout and returns to the console', async () => {
    localStorage.setItem(PROCESS_DRAFT_PREFIX + 'user-1:new', '{}');
    localStorage.setItem(PROCESS_DRAFT_PREFIX + 'user-1:7', '{}');
    localStorage.setItem('vv.language', 'es');
    await service.logout();

    expect(localStorage.getItem(PROCESS_DRAFT_PREFIX + 'user-1:new')).toBeNull();
    expect(localStorage.getItem(PROCESS_DRAFT_PREFIX + 'user-1:7')).toBeNull();
    expect(localStorage.getItem('vv.language')).toBe('es');
    expect(keycloak.logout).toHaveBeenCalledWith({ redirectUri: window.location.origin + '/admin' });
  });

  it('does not fail to clear drafts when the browser storage is blocked', () => {
    localStorage.setItem(PROCESS_DRAFT_PREFIX + 'user-1:new', '{}');
    vi.spyOn(Storage.prototype, 'key').mockImplementation(() => { throw new Error('blocked'); });

    expect(() => clearProcessDrafts()).not.toThrow();
  });
});
