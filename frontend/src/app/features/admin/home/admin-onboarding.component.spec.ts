import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { addIcons } from 'ionicons';
import { addOutline, barChartOutline, calendarOutline, documentTextOutline, helpCircleOutline } from 'ionicons/icons';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { AdminOnboardingComponent, ONBOARDING_KEY_PREFIX, ONBOARDING_STEPS } from './admin-onboarding.component';

type Onboarding = { open(): boolean; close(): void; reopen(): void };

describe('AdminOnboardingComponent («Primeros pasos»)', () => {
  const user = signal<string | null>('user-1');

  beforeEach(() => {
    localStorage.clear();
    user.set('user-1');
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideTranslateService(), { provide: AuthService, useValue: { userId: user } }],
    });
  });
  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  const create = () => TestBed.runInInjectionContext(() => new AdminOnboardingComponent()) as unknown as Onboarding;

  it('is open on the first visit and folded after «Entendido»', () => {
    const first = create();
    expect(first.open()).toBe(true);
    first.close();
    expect(first.open()).toBe(false);
    expect(localStorage.getItem(ONBOARDING_KEY_PREFIX + 'user-1')).not.toBeNull();
    expect(create().open()).toBe(false);
  });

  it('can be opened again with «Guía rápida»', () => {
    const page = create();
    page.close();
    page.reopen();
    expect(page.open()).toBe(true);
  });

  it('is shown to each person of a shared browser', () => {
    create().close();
    user.set('user-2');
    expect(create().open()).toBe(true);
  });

  it('links the four steps to the screens of the console menu', () => {
    expect(ONBOARDING_STEPS.map((s) => s.link)).toEqual([
      '/admin/procesos/nuevo', '/admin/procesos', '/admin/participacion', '/admin/resultados',
    ]);
  });

  it('renders the four steps with their links and folds into «Guía rápida»', async () => {
    // Registered here so that ion-icon does not try to fetch the SVG files in the test environment
    addIcons({ addOutline, barChartOutline, calendarOutline, documentTextOutline, helpCircleOutline });
    const fixture = TestBed.createComponent(AdminOnboardingComponent);
    fixture.detectChanges();
    const root: HTMLElement = fixture.nativeElement;

    expect(root.querySelector('section')?.getAttribute('aria-labelledby')).toBe('onboarding-title');
    expect([...root.querySelectorAll('ol.steps li a')].map((a) => a.getAttribute('href'))).toEqual(ONBOARDING_STEPS.map((s) => s.link));

    root.querySelector<HTMLButtonElement>('button.done')!.click();
    fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(root.querySelector('section')).toBeNull();
    expect(root.querySelector('button.reopen')?.getAttribute('aria-expanded')).toBe('false');

    root.querySelector<HTMLButtonElement>('button.reopen')!.click();
    fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(root.querySelectorAll('ol.steps li')).toHaveLength(4);
  });

  it('is shown and keeps nothing when the person is not known', () => {
    user.set(null);
    const page = create();
    expect(page.open()).toBe(true);
    page.close();
    expect(page.open()).toBe(false);
    expect(localStorage.length).toBe(0);
  });

  it('is simply shown again when the browser storage is blocked', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('blocked'); });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('blocked'); });
    const page = create();
    expect(page.open()).toBe(true);
    expect(() => page.close()).not.toThrow();
    expect(create().open()).toBe(true);
  });
});
