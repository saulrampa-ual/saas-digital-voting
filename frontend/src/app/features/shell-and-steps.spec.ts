import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { adminProviders } from '../../testing/admin-test-doubles';
import { App } from '../app';
import { FocusOnNavigationService } from '../core/a11y/focus-on-navigation.service';
import { ManagementRole } from '../core/auth/auth.service';
import { LanguageService } from '../core/i18n/language.service';
import { AdminShellComponent } from './admin/admin-shell.component';
import { NotFoundPage } from './not-found.page';
import { AccessCodeStepComponent } from './voter/steps/access-code-step.component';

/** Shell of the console, 404 page, root component, focus after navigation and the code step of the voter. */
describe('AdminShellComponent (menú de la consola)', () => {
  function entries(roles: ManagementRole[]): { path: string }[] {
    TestBed.configureTestingModule({ providers: adminProviders(roles) });
    const shell = TestBed.runInInjectionContext(() => new AdminShellComponent()) as unknown as { entries(): { path: string }[] };
    return shell.entries();
  }

  it('shows each role only its own entries', () => {
    const admin = entries(['ADMIN']).length;
    TestBed.resetTestingModule();
    const auditor = entries(['AUDITOR']).length;
    TestBed.resetTestingModule();
    const none = entries([]).length;
    expect(admin).toBeGreaterThan(0);
    expect(auditor).toBeGreaterThan(0);
    expect(none).toBeLessThan(admin);
  });
});

describe('NotFoundPage (404)', () => {
  for (const roles of [['ADMIN'], []] as ManagementRole[][]) {
    it(`renders for ${roles.length ? 'a signed-in person' : 'a visitor'}`, async () => {
      TestBed.configureTestingModule({ providers: adminProviders(roles) });
      const fixture = TestBed.createComponent(NotFoundPage);
      fixture.detectChanges();
      await fixture.whenStable();
      expect((fixture.nativeElement as HTMLElement).textContent).toContain('notFound');
    });
  }
});

describe('App (raíz)', () => {
  it('starts the language and the focus management', () => {
    const language = { init: vi.fn(), current: () => 'es' };
    const focus = { init: vi.fn() };
    TestBed.configureTestingModule({
      providers: [{ provide: LanguageService, useValue: language }, { provide: FocusOnNavigationService, useValue: focus }],
    });
    TestBed.runInInjectionContext(() => new App());
    expect(language.init).toHaveBeenCalled();
    expect(focus.init).toHaveBeenCalled();
  });
});

@Component({ template: '' })
class EmptyPage {}

describe('FocusOnNavigationService (foco tras navegar)', () => {
  afterEach(() => {
    vi.useRealTimers();
    document.body.innerHTML = '';
  });

  it('keeps the first load and then moves the focus to the heading of the visible page', async () => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({ providers: [provideRouter([{ path: 'a', component: EmptyPage }, { path: 'b', component: EmptyPage }])] });
    document.body.innerHTML = `
      <div class="ion-page ion-page-hidden"><h1 id="old">Anterior</h1></div>
      <div class="ion-page"><h1 id="new">Nueva</h1></div>`;
    TestBed.inject(FocusOnNavigationService).init();
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/a');
    await vi.advanceTimersByTimeAsync(400);
    expect(document.activeElement?.id).not.toBe('new');

    await router.navigateByUrl('/b');
    await vi.advanceTimersByTimeAsync(400);
    expect(document.activeElement?.id).toBe('new');
    expect(document.getElementById('new')?.getAttribute('tabindex')).toBe('-1');
  });

  it('does nothing when no visible page has a heading', async () => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({ providers: [provideRouter([{ path: 'a', component: EmptyPage }, { path: 'b', component: EmptyPage }])] });
    document.body.innerHTML = '<div class="ion-page"><p>Sin título</p></div>';
    TestBed.inject(FocusOnNavigationService).init();
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/a');
    await router.navigateByUrl('/b');
    await vi.advanceTimersByTimeAsync(400);
    expect(document.activeElement).toBe(document.body);
  });
});

describe('AccessCodeStepComponent (B1, código de acceso)', () => {
  type Step = {
    code: { set(v: string): void; (): string };
    secondsToResend(): number;
    minutesLeft(): number;
    countdown(): string;
    resendAnnouncement(): { key: string; params: { time?: string } };
    describeDigits(element: HTMLElement, invalid: boolean): Promise<void>;
    clear(): void;
    submit(): void;
    ngOnDestroy(): void;
  };

  function open(sent: { maskedEmail: string; expiresAt: string; resendAvailableAt: string } | null, busy = false) {
    TestBed.configureTestingModule({ providers: adminProviders([]) });
    const fixture = TestBed.createComponent(AccessCodeStepComponent);
    fixture.componentRef.setInput('sent', sent);
    fixture.componentRef.setInput('busy', busy);
    const codes: string[] = [];
    fixture.componentInstance.verify.subscribe((code) => codes.push(code));
    return { step: fixture.componentInstance as unknown as Step, codes, fixture };
  }

  it('counts down to the resend and shows the minutes left', async () => {
    const now = Date.now();
    const { step, fixture } = open({ maskedEmail: 'p***@example.com', expiresAt: new Date(now + 10 * 60_000).toISOString(),
      resendAvailableAt: new Date(now + 75_000).toISOString() });
    expect(step.secondsToResend()).toBeGreaterThan(70);
    expect(step.countdown()).toMatch(/^1:1\d$/);
    expect(step.minutesLeft()).toBe(10);
    fixture.detectChanges();
    await fixture.whenStable();
    step.ngOnDestroy();
  });

  it('has no countdown before the code is sent', () => {
    const { step } = open(null);
    expect(step.secondsToResend()).toBe(0);
    expect(step.minutesLeft()).toBe(0);
    expect(step.countdown()).toBe('0:00');
    step.ngOnDestroy();
  });

  it('sends only a full code and only when it is not busy, and clears the boxes', () => {
    const { step, codes } = open(null);
    step.code.set('123');
    step.submit();
    step.code.set('123456');
    step.submit();
    expect(codes).toEqual(['123456']);
    step.clear();
    expect(step.code()).toBe('');
    step.ngOnDestroy();

    TestBed.resetTestingModule();
    const busy = open(null, true);
    busy.step.code.set('123456');
    busy.step.submit();
    expect(busy.codes).toEqual([]);
    busy.step.ngOnDestroy();
  });

  it('names the six boxes in the interface language (IMP-67)', async () => {
    const { step } = open(null);
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('es', { voter: { code: { digit: 'Código de acceso, cifra {{position}} de {{total}}' } } });
    translate.use('es');
    const otp = document.createElement('div');
    for (let i = 0; i < 6; i++) {
      otp.appendChild(document.createElement('input')).setAttribute('aria-label', `Input ${i + 1} of 6`);
    }
    await step.describeDigits(otp, false);
    const labels = [...otp.querySelectorAll('input')].map((box) => box.getAttribute('aria-label'));
    expect(labels[0]).toBe('Código de acceso, cifra 1 de 6');
    expect(labels[5]).toBe('Código de acceso, cifra 6 de 6');
    step.ngOnDestroy();
  });

  it('links every box to the error after a wrong code and unlinks them when it is cleared (IMP-71)', async () => {
    const { step } = open(null);
    const otp = document.createElement('div');
    for (let i = 0; i < 6; i++) otp.appendChild(document.createElement('input'));
    const boxes = () => [...otp.querySelectorAll('input')];
    await step.describeDigits(otp, true);
    expect(boxes().every((box) => box.getAttribute('aria-invalid') === 'true')).toBe(true);
    expect(boxes().every((box) => box.getAttribute('aria-describedby') === 'code-error')).toBe(true);
    await step.describeDigits(otp, false);
    expect(boxes().some((box) => box.hasAttribute('aria-invalid') || box.hasAttribute('aria-describedby'))).toBe(false);
    step.ngOnDestroy();
  });

  it('keeps the visible countdown out of the live region and announces only the start and the end (IMP-68)', async () => {
    const now = Date.now();
    const waiting = open({ maskedEmail: 'p***@example.com', expiresAt: new Date(now + 10 * 60_000).toISOString(),
      resendAvailableAt: new Date(now + 75_000).toISOString() });
    const start = waiting.step.resendAnnouncement();
    expect(start.key).toBe('voter.code.resendIn');
    expect(start.params.time).toMatch(/^1:1\d$/);
    waiting.fixture.detectChanges();
    await waiting.fixture.whenStable();
    const element = waiting.fixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('[aria-live]').length).toBe(1);
    expect(element.querySelector('ion-note')?.hasAttribute('aria-live')).toBe(false);
    waiting.step.ngOnDestroy();

    TestBed.resetTestingModule();
    const ready = open({ maskedEmail: 'p***@example.com', expiresAt: new Date(now + 10 * 60_000).toISOString(),
      resendAvailableAt: new Date(now - 1_000).toISOString() });
    expect(ready.step.resendAnnouncement().key).toBe('voter.code.resendReady');
    ready.step.ngOnDestroy();
  });
});
