import { Component, effect, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { SessionTimeoutService } from '../../core/auth/session-timeout.service';

/**
 * D1 (IMP-15, WCAG 2.2.1): warning two minutes before the management session expires, with «Seguir aquí» to extend it,
 * and a notice once it has expired. A non-modal `alertdialog` at the bottom of the screen: it is announced and receives
 * the focus when it appears, but it never blocks the page. The countdown is not a live region (it would be read every
 * second); its value is in the accessible description read when the dialog appears.
 */
@Component({
  selector: 'app-session-timeout',
  imports: [TranslatePipe],
  template: `
    @if (session.state() !== 'active') {
      <div class="session" role="alertdialog" aria-modal="false" aria-labelledby="session-title" aria-describedby="session-text">
        @switch (session.state()) {
          @case ('expired') {
            <h2 id="session-title" tabindex="-1">{{ 'admin.session.expiredTitle' | translate }}</h2>
            <p id="session-text">{{ 'admin.session.expiredText' | translate }}</p>
            <div class="buttons">
              <button type="button" class="primary" (click)="session.login()">{{ 'admin.session.login' | translate }}</button>
            </div>
          }
          @case ('limit') {
            <h2 id="session-title" tabindex="-1">{{ 'admin.session.limitTitle' | translate }}</h2>
            <p id="session-text">{{ 'admin.session.limitText' | translate: { time: time() } }}</p>
            <div class="buttons">
              <button type="button" (click)="auth.logout()">{{ 'admin.logout' | translate }}</button>
            </div>
          }
          @default {
            <h2 id="session-title" tabindex="-1">{{ 'admin.session.warningTitle' | translate }}</h2>
            <p id="session-text">{{ 'admin.session.warningText' | translate: { time: time() } }}</p>
            <div class="buttons">
              <button type="button" class="primary" [disabled]="session.extending()" (click)="session.extend()">
                {{ 'admin.session.stay' | translate }}</button>
              <button type="button" (click)="auth.logout()">{{ 'admin.logout' | translate }}</button>
            </div>
          }
        }
      </div>
    }
  `,
  styles: `
    .session { position: fixed; z-index: 1000; left: 50%; bottom: 16px; transform: translateX(-50%); width: min(30rem, calc(100vw - 32px));
      background: #fff; color: var(--ion-text-color); border: 2px solid var(--ion-color-primary); border-radius: var(--vv-radius-card, 12px);
      padding: 16px 18px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25); }
    h2 { margin: 0 0 6px; font-size: 1.125rem; font-weight: 700; }
    h2:focus { outline: none; }
    p { margin: 0 0 12px; }
    .buttons { display: flex; flex-wrap: wrap; gap: 10px; }
    button { min-height: 44px; padding: 0 18px; border-radius: var(--vv-radius-control, 10px); font: inherit; font-weight: 600; cursor: pointer;
      background: #fff; color: var(--ion-color-primary); border: 1px solid var(--ion-color-primary); }
    button.primary { background: var(--ion-color-primary); color: #fff; }
    button:disabled { opacity: 0.6; cursor: default; }
    button:focus-visible { outline: 3px solid var(--ion-color-primary); outline-offset: 2px; }
  `,
})
export class SessionTimeoutComponent {
  protected readonly session = inject(SessionTimeoutService);
  protected readonly auth = inject(AuthService);
  private shown: string = 'active';

  constructor() {
    if (this.auth.authenticated()) this.session.start();
    // The focus moves to the dialog when it appears or changes, never on every tick of the countdown
    effect(() => {
      const state = this.session.state();
      if (state !== this.shown) {
        this.shown = state;
        if (state !== 'active') setTimeout(() => document.getElementById('session-title')?.focus(), 0);
      }
    });
  }

  /** «1 min 45 s» / «45 s». */
  protected time(): string {
    const left = Math.max(0, this.session.secondsLeft() ?? 0);
    const min = Math.floor(left / 60);
    const s = left % 60;
    return min ? `${min} min ${s} s` : `${s} s`;
  }
}
