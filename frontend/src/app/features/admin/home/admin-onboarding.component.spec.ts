import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { AdminOnboardingComponent, ONBOARDING_KEY_PREFIX, ONBOARDING_STEPS } from './admin-onboarding.component';

type Onboarding = { open(): boolean; close(): void; reopen(): void };

describe('AdminOnboardingComponent («Primeros pasos»)', () => {
  const user = signal<string | null>('user-1');

  beforeEach(() => {
    localStorage.clear();
    user.set('user-1');
    TestBed.configureTestingModule({ providers: [{ provide: AuthService, useValue: { userId: user } }] });
  });
  afterEach(() => localStorage.clear());

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
});
