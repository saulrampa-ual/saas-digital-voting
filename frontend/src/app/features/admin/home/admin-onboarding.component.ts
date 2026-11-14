import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonIcon } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';

/** Prefix of the «already seen» mark, one per person (Keycloak `sub`), kept in this browser (D-60). */
export const ONBOARDING_KEY_PREFIX = 'vv.onboarding.admin:';

/** The four steps of an ADMIN, in the order of the console menu. */
export const ONBOARDING_STEPS = [
  { key: 'create', icon: 'add-outline', link: '/admin/procesos/nuevo' },
  { key: 'schedule', icon: 'calendar-outline', link: '/admin/procesos' },
  { key: 'follow', icon: 'bar-chart-outline', link: '/admin/participacion' },
  { key: 'results', icon: 'document-text-outline', link: '/admin/resultados' },
] as const;

/**
 * «Primeros pasos» of the ADMIN home (instead of an external guide): the four basic steps with a link to each screen.
 * Shown open on the first visit of each person in this browser; «Entendido» folds it into a «Guía rápida» button that
 * opens it again at any time. Not a modal: it never blocks the page and is read in order by screen readers.
 */
@Component({
  selector: 'app-admin-onboarding',
  imports: [RouterLink, IonIcon, TranslatePipe],
  template: `
    @if (open()) {
      <section class="onboarding" aria-labelledby="onboarding-title">
        <div class="head">
          <h3 id="onboarding-title" tabindex="-1">{{ 'admin.onboarding.title' | translate }}</h3>
          <p>{{ 'admin.onboarding.intro' | translate }}</p>
        </div>
        <ol class="steps">
          @for (step of steps; track step.key; let i = $index) {
            <li>
              <span class="num" aria-hidden="true">{{ i + 1 }}</span>
              <span class="text">
                <strong>{{ 'admin.onboarding.steps.' + step.key + '.title' | translate }}</strong>
                <span>{{ 'admin.onboarding.steps.' + step.key + '.text' | translate }}</span>
                <a [routerLink]="step.link"><ion-icon [name]="step.icon" aria-hidden="true"></ion-icon>{{ 'admin.onboarding.steps.' + step.key + '.link' | translate }}</a>
              </span>
            </li>
          }
        </ol>
        <p class="note">{{ 'admin.onboarding.note' | translate }}</p>
        <button type="button" class="done" (click)="close()">{{ 'admin.onboarding.done' | translate }}</button>
      </section>
    } @else {
      <button type="button" class="reopen" (click)="reopen()" [attr.aria-expanded]="false">
        <ion-icon name="help-circle-outline" aria-hidden="true"></ion-icon>{{ 'admin.onboarding.reopen' | translate }}
      </button>
    }
  `,
  styles: `
    :host { display: block; margin-bottom: 24px; }
    .onboarding { background: #fff; border: 2px solid var(--ion-color-primary); border-radius: var(--vv-radius-card); padding: 20px; }
    .head h3 { margin: 0 0 4px; font-size: 1.25rem; font-weight: 700; }
    .head p { margin: 0 0 16px; color: var(--ion-color-medium); }
    .steps { list-style: none; margin: 0; padding: 0; display: grid; gap: 12px; grid-template-columns: minmax(0, 1fr); }
    @media (min-width: 900px) { .steps { grid-template-columns: repeat(4, minmax(0, 1fr)); } }
    .steps li { display: flex; gap: 12px; align-items: flex-start; background: #f7f9fb; border-radius: var(--vv-radius-control); padding: 12px; }
    .num { flex: none; display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; border-radius: 50%;
      background: var(--ion-color-primary); color: #fff; font-weight: 700; }
    .text { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
    .text a { display: inline-flex; align-items: center; gap: 6px; min-height: 44px; font-weight: 600; color: var(--ion-color-primary); }
    .note { margin: 16px 0 12px; font-size: 0.9375rem; color: var(--ion-color-medium); }
    button { display: inline-flex; align-items: center; gap: 8px; min-height: 44px; padding: 0 18px; border-radius: var(--vv-radius-control);
      font: inherit; font-weight: 600; cursor: pointer; }
    .done { background: var(--ion-color-primary); color: #fff; border: 0; }
    .reopen { background: #fff; color: var(--ion-color-primary); border: 1px solid #7b8794; }
    button:focus-visible, a:focus-visible { outline: 3px solid var(--ion-color-primary); outline-offset: 2px; }
    ion-icon { font-size: 20px; flex: none; }
  `,
})
export class AdminOnboardingComponent {
  private readonly auth = inject(AuthService);
  protected readonly steps = ONBOARDING_STEPS;
  private readonly key = computed(() => {
    const user = this.auth.userId();
    return user ? ONBOARDING_KEY_PREFIX + user : null;
  });
  protected readonly open = signal(!this.seen());

  protected close(): void {
    this.open.set(false);
    const key = this.key();
    if (!key) return;
    try {
      localStorage.setItem(key, new Date().toISOString());
    } catch {
      // Storage blocked: it will simply be shown again next time.
    }
    setTimeout(() => document.querySelector<HTMLElement>('app-admin-onboarding .reopen')?.focus(), 0);
  }

  protected reopen(): void {
    this.open.set(true);
    setTimeout(() => document.getElementById('onboarding-title')?.focus(), 0);
  }

  private seen(): boolean {
    const key = this.key();
    if (!key) return false;
    try {
      return localStorage.getItem(key) !== null;
    } catch {
      return false;
    }
  }
}
