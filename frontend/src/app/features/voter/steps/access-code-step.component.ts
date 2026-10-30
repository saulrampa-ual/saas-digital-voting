import { Component, computed, input, output, signal, OnDestroy } from '@angular/core';
import { IonButton, IonInputOtp, IonNote, IonSpinner, IonText } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { CodeSent } from '../../../core/voter/voter-api.service';

/**
 * B1 and V2: the code of 6 digits sent by email. Shows where it was sent and how long it lasts, the remaining
 * attempts after a wrong code and a countdown before a new code can be requested (RNF-14).
 */
@Component({
  selector: 'app-access-code-step',
  imports: [IonButton, IonInputOtp, IonNote, IonSpinner, IonText, TranslatePipe],
  template: `
    <h1 tabindex="-1">{{ 'voter.code.title' | translate }}</h1>
    @if (sent(); as info) {
      <p>{{ 'voter.code.sentTo' | translate: { email: info.maskedEmail, minutes: minutesLeft() } }}</p>
      <form (submit)="$event.preventDefault(); submit()">
        <label id="code-label" class="label">{{ 'voter.code.label' | translate }}</label>
        <ion-input-otp
          aria-labelledby="code-label"
          [attr.aria-describedby]="error() ? 'code-error' : null"
          [attr.aria-invalid]="error() ? 'true' : null"
          [length]="6"
          type="number"
          inputmode="numeric"
          fill="outline"
          [value]="code()"
          [disabled]="busy()"
          (ionInput)="code.set(($event.detail.value ?? '').toString())"
          (ionComplete)="code.set(($event.detail.value ?? '').toString()); submit()"
        ></ion-input-otp>
        @if (error()) {
          <ion-text color="danger"><p id="code-error" class="error" role="alert">{{ error() }}</p></ion-text>
        }
        <ion-button type="submit" expand="block" color="secondary" [disabled]="busy() || code().length !== 6">
          @if (busy()) {
            <ion-spinner name="crescent" aria-hidden="true"></ion-spinner>&nbsp;{{ 'voter.code.checking' | translate }}
          } @else {
            {{ 'voter.code.verify' | translate }}
          }
        </ion-button>
      </form>
      <div class="resend">
        @if (secondsToResend() > 0) {
          <ion-note aria-live="polite">{{ 'voter.code.resendIn' | translate: { time: countdown() } }}</ion-note>
        } @else {
          <ion-button fill="clear" [disabled]="busy()" (click)="resend.emit()">{{ 'voter.code.resend' | translate }}</ion-button>
        }
      </div>
    } @else {
      <p>{{ 'voter.code.intro' | translate }}</p>
      @if (error()) {
        <ion-text color="danger"><p class="error" role="alert">{{ error() }}</p></ion-text>
      }
      <ion-button expand="block" color="secondary" [disabled]="busy()" (click)="resend.emit()">
        @if (busy()) {
          <ion-spinner name="crescent" aria-hidden="true"></ion-spinner>&nbsp;{{ 'voter.code.sending' | translate }}
        } @else {
          {{ 'voter.code.send' | translate }}
        }
      </ion-button>
    }
  `,
  styles: `
    .label { display: block; font-weight: 600; margin: 1rem 0 0.5rem; }
    ion-input-otp { --height: 56px; margin-bottom: 0.75rem; }
    ion-button { --min-height: 52px; }
    .error { margin: 0 0 0.75rem; }
    .resend { margin-top: 0.75rem; text-align: center; }
  `,
})
export class AccessCodeStepComponent implements OnDestroy {
  readonly sent = input<CodeSent | null>(null);
  readonly busy = input(false);
  readonly error = input<string | null>(null);
  readonly resend = output<void>();
  readonly verify = output<string>();

  protected readonly code = signal('');
  private readonly now = signal(Date.now());
  private readonly timer = setInterval(() => this.now.set(Date.now()), 1000);

  protected readonly secondsToResend = computed(() => {
    const info = this.sent();
    return info ? Math.max(0, Math.ceil((Date.parse(info.resendAvailableAt) - this.now()) / 1000)) : 0;
  });
  protected readonly minutesLeft = computed(() => {
    const info = this.sent();
    return info ? Math.max(1, Math.round((Date.parse(info.expiresAt) - this.now()) / 60000)) : 0;
  });
  protected readonly countdown = computed(() => {
    const seconds = this.secondsToResend();
    return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;
  });

  /** Clears the boxes after a wrong code so the voter can type again (V2). */
  clear(): void {
    this.code.set('');
  }

  protected submit(): void {
    if (this.code().length === 6 && !this.busy()) {
      this.verify.emit(this.code());
    }
  }

  ngOnDestroy(): void {
    clearInterval(this.timer);
  }
}
