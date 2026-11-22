import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { adminProviders } from '../../../testing/admin-test-doubles';
import { VoterApiError, VoterApiService } from '../../core/voter/voter-api.service';
import { SsoError, VoterSsoService } from '../../core/voter/voter-sso.service';
import { VoterSsoCallbackPage } from './voter-sso-callback.page';

type Page = { ngOnInit(): Promise<void>; failed(): boolean };

const PENDING = { linkToken: 'personal-link', verifier: 'v', state: 's', nonce: 'n', purpose: 'vote' as const, startedAt: 0 };

/**
 * Return from the institutional account (RF-05b, D-48; hidden in the console while it cannot be tested, A9): the page
 * hands the outcome to the voter flow and goes back to the personal link. Synthetic tokens only.
 */
describe('VoterSsoCallbackPage (vuelta de la cuenta institucional)', () => {
  let sso: { complete: ReturnType<typeof vi.fn>; handOff: ReturnType<typeof vi.fn> };
  let api: { verifyInstitutionalAccount: ReturnType<typeof vi.fn> };
  let navigate: ReturnType<typeof vi.fn>;
  let page: Page;

  beforeEach(() => {
    sso = {
      complete: vi.fn().mockResolvedValue({ linkToken: 'personal-link', idToken: 'id-token', purpose: 'vote' }),
      handOff: vi.fn(),
    };
    api = { verifyInstitutionalAccount: vi.fn().mockResolvedValue({ hasVoted: false }) };
    TestBed.configureTestingModule({
      providers: [...adminProviders([]), { provide: VoterSsoService, useValue: sso }, { provide: VoterApiService, useValue: api }],
    });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true) as unknown as ReturnType<typeof vi.fn>;
    page = TestBed.runInInjectionContext(() => new VoterSsoCallbackPage()) as unknown as Page;
  });

  it('verifies the account and goes back to the personal link with the access granted', async () => {
    await page.ngOnInit();
    expect(api.verifyInstitutionalAccount).toHaveBeenCalledWith('personal-link', 'id-token');
    expect(sso.handOff).toHaveBeenCalledWith({ kind: 'granted', granted: { hasVoted: false }, purpose: 'vote' });
    expect(navigate).toHaveBeenCalledWith('/votar/personal-link', { replaceUrl: true });
  });

  it('hands over the refusal of voter-access (another account, invalid token)', async () => {
    api.verifyInstitutionalAccount.mockRejectedValueOnce(new VoterApiError(403, { code: 'OTHER_ACCOUNT' }));
    await page.ngOnInit();
    expect(sso.handOff).toHaveBeenCalledWith({ kind: 'error', status: 403, problem: { code: 'OTHER_ACCOUNT' }, purpose: 'vote' });
    api.verifyInstitutionalAccount.mockRejectedValueOnce(new Error('offline'));
    await page.ngOnInit();
    expect(sso.handOff).toHaveBeenLastCalledWith({ kind: 'error', status: -1, problem: {}, purpose: 'vote' });
  });

  it('goes back to the link when the login was cancelled or failed, and fails without a pending login', async () => {
    sso.complete.mockRejectedValueOnce(new SsoError('cancelled', PENDING));
    await page.ngOnInit();
    expect(sso.handOff).toHaveBeenCalledWith({ kind: 'error', status: 0, problem: {}, purpose: 'vote' });
    sso.complete.mockRejectedValueOnce(new SsoError('failed', PENDING));
    await page.ngOnInit();
    expect(sso.handOff).toHaveBeenLastCalledWith({ kind: 'error', status: 401, problem: {}, purpose: 'vote' });
    sso.complete.mockRejectedValueOnce(new SsoError('expired', null));
    await page.ngOnInit();
    expect(page.failed()).toBe(true);
  });
});
