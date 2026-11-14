import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';
import { SESSION_WARNING_SECONDS, SessionTimeoutService } from './session-timeout.service';

describe('SessionTimeoutService (D1)', () => {
  const now = Date.UTC(2026, 10, 13, 12, 0, 0);
  const sec = now / 1000;
  let keycloak: { refreshTokenParsed?: { exp: number }; timeSkew: number; updateToken: ReturnType<typeof vi.fn>; login: ReturnType<typeof vi.fn> };
  let service: SessionTimeoutService;

  beforeEach(() => {
    keycloak = { refreshTokenParsed: { exp: sec + 1800 }, timeSkew: 0, updateToken: vi.fn(), login: vi.fn() };
    TestBed.configureTestingModule({ providers: [{ provide: Keycloak, useValue: keycloak }] });
    service = TestBed.inject(SessionTimeoutService);
  });

  it('stays quiet while more than two minutes are left', () => {
    service.check(now);
    expect(service.state()).toBe('active');
    expect(service.secondsLeft()).toBe(1800);
  });

  it('warns two minutes before the end and reports expiry', () => {
    keycloak.refreshTokenParsed = { exp: sec + SESSION_WARNING_SECONDS };
    service.check(now);
    expect(service.state()).toBe('warning');
    service.check(now + (SESSION_WARNING_SECONDS + 1) * 1000);
    expect(service.state()).toBe('expired');
  });

  it('takes the clock difference with the server into account', () => {
    keycloak.refreshTokenParsed = { exp: sec + 300 };
    keycloak.timeSkew = -200; // browser 200 s ahead of the server
    service.check(now);
    expect(service.secondsLeft()).toBe(100);
    expect(service.state()).toBe('warning');
  });

  it('«Seguir aquí» refreshes the token and the warning goes away', async () => {
    keycloak.refreshTokenParsed = { exp: sec + 60 };
    service.check(now);
    keycloak.updateToken.mockImplementation(async () => { keycloak.refreshTokenParsed = { exp: Date.now() / 1000 + 1800 }; return true; });
    await service.extend();
    expect(keycloak.updateToken).toHaveBeenCalledWith(-1);
    expect(service.state()).toBe('active');
  });

  it('says the session cannot be extended when its maximum lifespan is reached', async () => {
    keycloak.updateToken.mockImplementation(async () => { keycloak.refreshTokenParsed = { exp: Date.now() / 1000 + 60 }; return true; });
    keycloak.refreshTokenParsed = { exp: Date.now() / 1000 + 60 };
    await service.extend();
    expect(service.state()).toBe('limit');
  });

  it('is inactive without a session', () => {
    keycloak.refreshTokenParsed = undefined;
    service.check(now);
    expect(service.state()).toBe('active');
    expect(service.secondsLeft()).toBeNull();
  });
});
