import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth/auth.service';
import { SessionState, SessionTimeoutService } from '../../core/auth/session-timeout.service';
import { SessionTimeoutComponent } from './session-timeout.component';

/** D1 (IMP-15, WCAG 2.2.1): the warning before the management session expires, rendered with service doubles. */
describe('SessionTimeoutComponent (D1)', () => {
  let session: {
    state: ReturnType<typeof signal<SessionState>>; secondsLeft: ReturnType<typeof signal<number | null>>;
    extending: ReturnType<typeof signal<boolean>>; start: ReturnType<typeof vi.fn>; extend: ReturnType<typeof vi.fn>;
    login: ReturnType<typeof vi.fn>;
  };
  let auth: { authenticated: ReturnType<typeof signal<boolean>>; logout: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    session = {
      state: signal<SessionState>('active'), secondsLeft: signal<number | null>(null), extending: signal(false),
      start: vi.fn(), extend: vi.fn(), login: vi.fn(),
    };
    auth = { authenticated: signal(true), logout: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        { provide: SessionTimeoutService, useValue: session },
        { provide: AuthService, useValue: auth },
      ],
    });
  });

  function render() {
    const fixture = TestBed.createComponent(SessionTimeoutComponent);
    fixture.detectChanges();
    const root: HTMLElement = fixture.nativeElement;
    return {
      fixture,
      dialog: () => root.querySelector<HTMLElement>('[role="alertdialog"]'),
      buttons: () => [...root.querySelectorAll<HTMLButtonElement>('button')],
      time: () => (fixture.componentInstance as unknown as { time(): string }).time(),
    };
  }

  it('starts watching the session of a signed-in user and shows nothing while it is active', () => {
    const view = render();
    expect(session.start).toHaveBeenCalledTimes(1);
    expect(view.dialog()).toBeNull();
  });

  it('does not watch anything without a session', () => {
    auth.authenticated.set(false);
    render();
    expect(session.start).not.toHaveBeenCalled();
  });

  it('warns with a non-modal dialog that receives the focus and lets the user stay or leave', async () => {
    const view = render();
    session.secondsLeft.set(105);
    session.state.set('warning');
    view.fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(view.dialog()?.getAttribute('aria-modal')).toBe('false');
    expect(document.activeElement?.id).toBe('session-title');
    expect(view.time()).toBe('1 min 45 s');
    const [stay, logout] = view.buttons();
    stay.click();
    logout.click();
    expect(session.extend).toHaveBeenCalledTimes(1);
    expect(auth.logout).toHaveBeenCalledTimes(1);
  });

  it('disables «Seguir aquí» while the session is being extended and shows seconds only under a minute', () => {
    const view = render();
    session.secondsLeft.set(45);
    session.extending.set(true);
    session.state.set('warning');
    view.fixture.detectChanges();

    expect(view.buttons()[0].disabled).toBe(true);
    expect(view.time()).toBe('45 s');
  });

  it('only offers to log out when the session cannot be extended any further', () => {
    const view = render();
    session.secondsLeft.set(30);
    session.state.set('limit');
    view.fixture.detectChanges();

    expect(view.buttons()).toHaveLength(1);
    view.buttons()[0].click();
    expect(auth.logout).toHaveBeenCalledTimes(1);
    expect(session.extend).not.toHaveBeenCalled();
  });

  it('offers to sign in again once the session has expired', () => {
    const view = render();
    session.secondsLeft.set(-5);
    session.state.set('expired');
    view.fixture.detectChanges();

    expect(view.time()).toBe('0 s');
    expect(view.buttons()).toHaveLength(1);
    view.buttons()[0].click();
    expect(session.login).toHaveBeenCalledTimes(1);
  });

  it('hides the dialog again when the session is extended', () => {
    const view = render();
    session.secondsLeft.set(60);
    session.state.set('warning');
    view.fixture.detectChanges();
    session.secondsLeft.set(null);
    session.state.set('active');
    view.fixture.detectChanges();

    expect(view.dialog()).toBeNull();
    expect(view.time()).toBe('0 s');
  });
});
