import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { addIcons } from 'ionicons';
import { timeOutline } from 'ionicons/icons';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { LanguageService } from '../../core/i18n/language.service';
import {
  AccessGranted, Ballot, CodeSent, LinkStatus, VoterApiError, VoterApiService,
} from '../../core/voter/voter-api.service';
import { SsoOutcome, VoterSsoService } from '../../core/voter/voter-sso.service';
import { VoterFlowPage } from './voter-flow.page';

/** The page's protected members, reached from the test. */
type Page = {
  screen(): string;
  step(): number;
  purpose(): string;
  busy(): boolean;
  codeSent(): CodeSent | null;
  codeError(): string | null;
  ssoError(): string | null;
  lockedUntil(): string | null;
  retryAfter(): number | null;
  votedOn(): string | null;
  receiptCode(): string | null;
  verification(): string;
  showStatusBar(): boolean;
  choice: { set(value: number | 'blank' | null): void; (): number | 'blank' | null };
  ngOnInit(): Promise<void>;
  load(): Promise<void>;
  start(): void;
  startResults(): void;
  sendCode(): Promise<void>;
  verifyCode(code: string): Promise<void>;
  review(): void;
  changeSelection(): void;
  confirm(): Promise<void>;
  enterAgain(): void;
};

const HOUR = 3_600_000;
const link = (over: Partial<LinkStatus> = {}): LinkStatus => ({
  processId: 12,
  title: { es: 'Elección de prueba' },
  status: 'OPEN',
  opensAt: new Date(Date.now() - HOUR).toISOString(),
  closesAt: new Date(Date.now() + HOUR).toISOString(),
  defaultLanguage: 'es',
  authentication: 'EMAIL_CODE',
  ...over,
});
const SENT: CodeSent = { maskedEmail: 'p***@verivoto.test', expiresAt: '2026-11-16T10:10:00Z', resendAvailableAt: '2026-11-16T10:01:00Z' };
const GRANTED: AccessGranted = { hasVoted: false, votedOn: null, votingToken: 'voting-token', votingTokenExpiresAt: '2026-11-16T10:30:00Z' };
const BALLOT: Ballot = {
  processId: 12, voteType: 'SINGLE_CHOICE', question: { es: '¿Opción?' },
  options: [{ id: 1, position: 1, text: { es: 'Opción A' } }, { id: 2, position: 2, text: { es: 'Opción B' } }],
  blankVoteEnabled: true, closesAt: new Date(Date.now() + HOUR).toISOString(),
};
const failure = (status: number, problem: Record<string, unknown> = {}, retryAfter: number | null = null) =>
  new VoterApiError(status, problem, retryAfter);

/**
 * Voter flow (RF-08, RF-09, RF-10; screens V1–V7, B1–B4, C2–C4): the state machine of the page with the API
 * replaced by a double. Synthetic data only; without translations loaded, texts are their keys.
 */
describe('VoterFlowPage (flujo del votante)', () => {
  let api: Record<'linkStatus' | 'sendCode' | 'verifyCode' | 'ballot' | 'castVote' | 'result' | 'countedVotes',
    ReturnType<typeof vi.fn>>;
  let sso: { take: ReturnType<typeof vi.fn>; start: ReturnType<typeof vi.fn> };
  let fixture: ComponentFixture<VoterFlowPage>;

  beforeEach(() => {
    api = {
      linkStatus: vi.fn().mockResolvedValue(link()),
      sendCode: vi.fn().mockResolvedValue(SENT),
      verifyCode: vi.fn().mockResolvedValue(GRANTED),
      ballot: vi.fn().mockResolvedValue(BALLOT),
      castVote: vi.fn().mockResolvedValue({ receiptCode: 'ABCD-1234' }),
      result: vi.fn(),
      countedVotes: vi.fn(),
    };
    sso = { take: vi.fn().mockReturnValue(null), start: vi.fn().mockResolvedValue(undefined) };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideTranslateService(),
        { provide: VoterApiService, useValue: api },
        { provide: VoterSsoService, useValue: sso },
        { provide: LanguageService, useValue: { current: signal('es') } },
      ],
    });
  });
  afterEach(() => vi.restoreAllMocks());

  async function open(): Promise<Page> {
    fixture = TestBed.createComponent(VoterFlowPage);
    fixture.componentRef.setInput('linkToken', 'personal-link');
    const page = fixture.componentInstance as unknown as Page;
    await page.ngOnInit();
    return page;
  }

  /** Up to the ballot: link, code sent and code accepted. */
  async function atBallot(): Promise<Page> {
    const page = await open();
    page.start();
    await page.verifyCode('123456');
    return page;
  }

  describe('personal link', () => {
    it('shows the welcome screen of an open process, with the status bar and no step yet', async () => {
      const page = await open();
      expect(api.linkStatus).toHaveBeenCalledWith('personal-link');
      expect(page.screen()).toBe('welcome');
      expect(page.step()).toBe(0);
      expect(page.showStatusBar()).toBe(true);
    });

    it.each([
      ['SCHEDULED', 'notOpen'], ['CLOSED', 'closed'], ['TALLIED', 'resultsReady'], ['ARCHIVED', 'resultsReady'],
    ] as const)('shows the state of a process that is %s', async (status, screen) => {
      api.linkStatus.mockResolvedValue(link({ status }));
      expect((await open()).screen()).toBe(screen);
    });

    it('treats an open process past its closing time as closed', async () => {
      api.linkStatus.mockResolvedValue(link({ closesAt: new Date(Date.now() - 1000).toISOString() }));
      expect((await open()).screen()).toBe('closed');
    });

    it.each([[404, 'invalidLink'], [410, 'expiredLink'], [500, 'error'], [0, 'error']])(
      'maps the error %i of the link to its screen', async (status, screen) => {
        api.linkStatus.mockRejectedValue(failure(status));
        expect((await open()).screen()).toBe(screen);
      });

    it('asks to wait after a 429 of the gateway and loads the link again with «Reintentar» (IMP-50)', async () => {
      api.linkStatus.mockRejectedValueOnce(failure(429, {}, 30));
      const page = await open();
      expect(page.screen()).toBe('tooMany');
      expect(page.retryAfter()).toBe(30);
      await page.load();
      expect(api.linkStatus).toHaveBeenCalledTimes(2);
      expect(page.screen()).toBe('welcome');
    });
  });

  describe('access code (B1, V2)', () => {
    it('goes to the code step and sends the first code right away', async () => {
      const page = await open();
      page.start();
      await Promise.resolve();
      await Promise.resolve();
      expect(page.screen()).toBe('code');
      expect(page.step()).toBe(1);
      expect(api.sendCode).toHaveBeenCalledWith('personal-link');
      expect(page.codeSent()).toEqual(SENT);
      expect(page.busy()).toBe(false);
    });

    it.each([
      [failure(429, { resendAvailableAt: '2026-11-16T10:01:00Z' }), 'voter.code.tooSoon'],
      [failure(429, {}, 20), 'voter.tooMany.wait'],
      [failure(429), 'voter.tooMany.soon'],
      [failure(503), 'voter.code.mailFailed'],
      [failure(0), 'voter.code.mailFailed'],
    ])('explains why the code was not sent (%#)', async (error, message) => {
      api.sendCode.mockRejectedValue(error);
      const page = await open();
      await page.sendCode();
      expect(page.codeError()).toBe(message);
      expect(page.busy()).toBe(false);
    });

    it('shows the ballot after a valid code', async () => {
      const page = await atBallot();
      expect(api.verifyCode).toHaveBeenCalledWith('personal-link', '123456');
      expect(api.ballot).toHaveBeenCalledWith(12, 'voting-token');
      expect(page.screen()).toBe('ballot');
      expect(page.step()).toBe(2);
    });

    it.each([
      [failure(400, { remainingAttempts: 2 }), 'voter.code.wrong'],
      [failure(410), 'voter.code.expired'],
      [failure(429, {}, 15), 'voter.tooMany.wait'],
    ])('stays on the code step with a message when the code is not accepted (%#)', async (error, message) => {
      api.verifyCode.mockRejectedValue(error);
      const page = await open();
      page.start();
      await page.verifyCode('000000');
      expect(page.screen()).toBe('code');
      expect(page.codeError()).toBe(message);
      expect(api.ballot).not.toHaveBeenCalled();
    });

    it('shows the lock and when it ends after too many wrong codes', async () => {
      api.verifyCode.mockRejectedValue(failure(423, { lockedUntil: '2026-11-16T10:15:00Z' }));
      const page = await open();
      page.start();
      await page.verifyCode('000000');
      expect(page.screen()).toBe('locked');
      expect(page.lockedUntil()).toBe('2026-11-16T10:15:00Z');
    });

    it('says «already voted» with the day, without giving a ballot', async () => {
      api.verifyCode.mockResolvedValue({ ...GRANTED, hasVoted: true, votedOn: '2026-11-15', votingToken: null });
      const page = await open();
      page.start();
      await page.verifyCode('123456');
      expect(page.screen()).toBe('alreadyVoted');
      expect(page.votedOn()).toBe('2026-11-15');
      expect(api.ballot).not.toHaveBeenCalled();
    });

    it.each([[409, 'alreadyVoted'], [404, 'sessionExpired'], [410, 'sessionExpired'], [422, 'closed'], [500, 'error']])(
      'maps the error %i of the ballot to its screen', async (status, screen) => {
        api.ballot.mockRejectedValue(failure(status));
        expect((await atBallot()).screen()).toBe(screen);
      });

    it('reports the process as closed when it closes while the person enters the code', async () => {
      api.verifyCode.mockRejectedValue(failure(422, { processStatus: 'CLOSED' }));
      const page = await open();
      page.start();
      await page.verifyCode('123456');
      expect(page.screen()).toBe('closed');
    });
  });

  describe('vote and receipt (B2–B4, V3, V6, V7)', () => {
    it('goes from the ballot to the review and back without losing the selection', async () => {
      const page = await atBallot();
      page.choice.set(2);
      page.review();
      expect(page.screen()).toBe('review');
      expect(page.step()).toBe(3);
      page.changeSelection();
      expect(page.screen()).toBe('ballot');
      expect(page.choice()).toBe(2);
    });

    it('casts the selected option once and shows the receipt', async () => {
      const page = await atBallot();
      page.choice.set(2);
      await page.confirm();
      expect(api.castVote).toHaveBeenCalledWith(12, 'voting-token', { optionId: 2 });
      expect(page.screen()).toBe('receipt');
      expect(page.step()).toBe(4);
      expect(page.receiptCode()).toBe('ABCD-1234');
      expect(page.showStatusBar()).toBe(false);
      await page.confirm();
      expect(api.castVote).toHaveBeenCalledTimes(1);
    });

    it('casts a blank vote as such', async () => {
      const page = await atBallot();
      page.choice.set('blank');
      await page.confirm();
      expect(api.castVote).toHaveBeenCalledWith(12, 'voting-token', { blank: true });
    });

    it('does not vote without a selection', async () => {
      const page = await atBallot();
      await page.confirm();
      expect(api.castVote).not.toHaveBeenCalled();
      expect(page.screen()).toBe('ballot');
    });

    it.each([[409, 'alreadyVoted'], [404, 'sessionExpired'], [410, 'sessionExpired'], [422, 'closed'], [400, 'ballot']])(
      'maps the error %i of the vote to its screen', async (status, screen) => {
        api.castVote.mockRejectedValue(failure(status));
        const page = await atBallot();
        page.choice.set(1);
        await page.confirm();
        expect(page.screen()).toBe(screen);
        expect(page.busy()).toBe(false);
      });

    it('retries a vote that was not recorded with the same token and selection (V7)', async () => {
      api.castVote.mockRejectedValueOnce(failure(503));
      const page = await atBallot();
      page.choice.set(1);
      await page.confirm();
      expect(page.screen()).toBe('notRecorded');
      expect(page.step()).toBe(3);
      await page.confirm();
      expect(api.castVote).toHaveBeenCalledTimes(2);
      expect(api.castVote.mock.calls[1]).toEqual(api.castVote.mock.calls[0]);
      expect(page.screen()).toBe('receipt');
    });

    it('goes back to the code step with the same link after the session expires (V6)', async () => {
      api.castVote.mockRejectedValue(failure(410));
      const page = await atBallot();
      page.choice.set(1);
      await page.confirm();
      page.enterAgain();
      expect(page.screen()).toBe('code');
      expect(page.choice()).toBeNull();
      expect(page.codeSent()).toBeNull();
      await page.confirm();
      expect(api.castVote).toHaveBeenCalledTimes(1);
    });
  });

  describe('result with the personal link (D-41)', () => {
    const RESULT = { processId: 12 };

    it('shows the result after the code and marks it as not verified when the votes cannot be read', async () => {
      api.linkStatus.mockResolvedValue(link({ status: 'TALLIED' }));
      api.verifyCode.mockResolvedValue({ ...GRANTED, votingToken: null, resultsToken: 'results-token' });
      api.result.mockResolvedValue(RESULT);
      api.countedVotes.mockRejectedValue(failure(500));
      const page = await open();
      page.startResults();
      await page.verifyCode('123456');
      expect(page.purpose()).toBe('results');
      expect(api.result).toHaveBeenCalledWith(12, 'results-token');
      expect(page.screen()).toBe('results');
      expect(page.step()).toBe(0);
      await vi.waitFor(() => expect(page.verification()).toBe('error'));
      expect(api.ballot).not.toHaveBeenCalled();
    });

    it.each([[410, 'sessionExpired'], [500, 'error']])('maps the error %i of the result to its screen',
      async (status, screen) => {
        api.linkStatus.mockResolvedValue(link({ status: 'TALLIED' }));
        api.verifyCode.mockResolvedValue({ ...GRANTED, votingToken: null, resultsToken: 'results-token' });
        api.result.mockRejectedValue(failure(status));
        const page = await open();
        page.startResults();
        await page.verifyCode('123456');
        expect(page.screen()).toBe(screen);
      });
  });

  describe('institutional account (RF-05b)', () => {
    const sso_link = () => api.linkStatus.mockResolvedValue(link({ authentication: 'INSTITUTIONAL_SSO' }));
    const back = (outcome: SsoOutcome) => sso.take.mockReturnValueOnce(outcome);

    it('asks for the account instead of sending a code', async () => {
      sso_link();
      const page = await open();
      page.start();
      expect(page.screen()).toBe('sso');
      expect(page.step()).toBe(1);
      expect(api.sendCode).not.toHaveBeenCalled();
    });

    it('shows the ballot when the account is the person of the link', async () => {
      sso_link();
      back({ kind: 'granted', granted: GRANTED, purpose: 'vote' });
      const page = await open();
      expect(page.screen()).toBe('ballot');
    });

    it.each([
      [403, { remainingAttempts: 2 }, 'voter.sso.mismatch'],
      [0, {}, 'voter.sso.cancelled'],
      [401, {}, 'voter.sso.invalid'],
      [429, {}, 'voter.tooMany.soon'],
    ])('explains the error %i of the account on the same screen', async (status, problem, message) => {
      sso_link();
      back({ kind: 'error', status, problem, purpose: 'vote' });
      const page = await open();
      expect(page.screen()).toBe('sso');
      expect(page.ssoError()).toBe(message);
    });

    it('shows the lock when the account failed too many times', async () => {
      sso_link();
      back({ kind: 'error', status: 423, problem: { lockedUntil: '2026-11-16T10:15:00Z' }, purpose: 'vote' });
      expect((await open()).screen()).toBe('locked');
    });
  });

  it('renders the wait message as an alert with a retry button (IMP-50)', async () => {
    // Registered here so that ion-icon does not try to fetch the SVG file in the test environment
    addIcons({ timeOutline });
    api.linkStatus.mockRejectedValueOnce(failure(429, {}, 30));
    // The first change detection runs ngOnInit, as the router does
    fixture = TestBed.createComponent(VoterFlowPage);
    fixture.componentRef.setInput('linkToken', 'personal-link');
    fixture.detectChanges();
    await vi.waitFor(() => expect((fixture.componentInstance as unknown as Page).screen()).toBe('tooMany'));
    fixture.detectChanges();
    const main = fixture.nativeElement as HTMLElement;
    const alert = main.querySelector('[role="alert"]');
    expect(alert?.querySelector('h1')?.textContent).toContain('voter.tooMany.title');
    expect(alert?.textContent).toContain('voter.tooMany.wait');
    const retry = alert?.querySelector('ion-button') as HTMLElement;
    expect(retry.textContent).toContain('voter.states.retry');
    retry.click();
    await vi.waitFor(() => expect((fixture.componentInstance as unknown as Page).screen()).toBe('welcome'));
    expect(api.linkStatus).toHaveBeenCalledTimes(2);
  });
});
