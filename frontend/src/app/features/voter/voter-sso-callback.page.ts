import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { IonContent, IonSpinner } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { VoterApiError, VoterApiService } from '../../core/voter/voter-api.service';
import { SsoError, VoterSsoService } from '../../core/voter/voter-sso.service';

/**
 * RF-05b (D-48): return from the institutional account. Exchanges the code, asks voter-access whether the account is
 * the person of the link and goes back to the personal link with the outcome in memory. Nothing shows the link.
 */
@Component({
  selector: 'app-voter-sso-callback',
  imports: [IonContent, IonSpinner, TranslatePipe],
  template: `
    <ion-content class="ion-padding">
      <main class="center">
        @if (failed()) {
          <h1 tabindex="-1">{{ 'voter.sso.failedTitle' | translate }}</h1>
          <p>{{ 'voter.sso.failedText' | translate }}</p>
        } @else {
          <ion-spinner role="status" [attr.aria-label]="'voter.sso.checking' | translate" />
          <p>{{ 'voter.sso.checking' | translate }}</p>
        }
      </main>
    </ion-content>
  `,
  styles: `.center { max-width: 480px; margin: 64px auto; text-align: center; }`,
})
export class VoterSsoCallbackPage implements OnInit {
  private readonly sso = inject(VoterSsoService);
  private readonly api = inject(VoterApiService);
  private readonly router = inject(Router);
  protected readonly failed = signal(false);

  async ngOnInit(): Promise<void> {
    const query = new URLSearchParams(window.location.search);
    // The code and state leave the address bar and the history right away
    history.replaceState(null, '', '/votar/sso');
    let login: { linkToken: string; idToken: string; purpose: 'vote' | 'results' };
    try {
      login = await this.sso.complete(query);
    } catch (error) {
      const pending = error instanceof SsoError ? error.pending : null;
      if (!pending) {
        this.failed.set(true);
        return;
      }
      this.sso.handOff({ kind: 'error', status: error instanceof SsoError && error.reason === 'cancelled' ? 0 : 401,
        problem: {}, purpose: pending.purpose });
      await this.router.navigateByUrl(`/votar/${pending.linkToken}`, { replaceUrl: true });
      return;
    }
    try {
      const granted = await this.api.verifyInstitutionalAccount(login.linkToken, login.idToken);
      this.sso.handOff({ kind: 'granted', granted, purpose: login.purpose });
    } catch (error) {
      this.sso.handOff({ kind: 'error', status: error instanceof VoterApiError ? error.status : -1,
        problem: error instanceof VoterApiError ? error.problem : {}, purpose: login.purpose });
    }
    await this.router.navigateByUrl(`/votar/${login.linkToken}`, { replaceUrl: true });
  }
}
