import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
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
});
