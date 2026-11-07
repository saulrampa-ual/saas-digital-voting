import { Component, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import {
  IonButton, IonButtons, IonContent, IonFooter, IonHeader, IonIcon, IonInput, IonSpinner, IonText, IonTitle,
  IonToolbar,
} from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { VoterApiError, VoterApiService } from '../../core/voter/voter-api.service';
import { LanguageSwitcherComponent } from '../../shared/language-switcher/language-switcher.component';

type Result = 'present' | 'absent' | 'unknownProcess' | 'error' | null;

/**
 * B5: anyone with a receipt code can check that it is in the ballot box (RF-10). It never shows the vote. The code
 * arrives through the navigation state from B4 (not in the URL) or is typed by the voter.
 */
@Component({
  selector: 'app-receipt-check',
  imports: [
    IonHeader, IonToolbar, IonTitle, IonButtons, IonContent, IonFooter, IonInput, IonButton, IonSpinner, IonText,
    IonIcon, TranslatePipe, LanguageSwitcherComponent,
  ],
  template: `
    <ion-header>
      <ion-toolbar color="tertiary">
        <ion-title>VeriVoto</ion-title>
        <ion-buttons slot="end"><app-language-switcher /></ion-buttons>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <main class="voter">
        <h1 tabindex="-1">{{ 'voter.verify.title' | translate }}</h1>
        <p>{{ 'voter.verify.intro' | translate }}</p>
        <form (submit)="$event.preventDefault(); check()">
          <ion-input
            class="mono"
            fill="outline"
            labelPlacement="stacked"
            [label]="'voter.verify.label' | translate"
            [helperText]="'voter.verify.helper' | translate"
            [class.field-invalid]="formatError()"
            [value]="code()"
            (ionInput)="code.set(($event.detail.value ?? '').toString()); formatError.set(false); result.set(null)"
            autocomplete="off"
            spellcheck="false"
          ></ion-input>
          <!-- DEF-06 (WCAG 3.3.1): own error element; Ionic's errorText depends on the ion-invalid class, which Ionic
               drops when it re-renders the host. role="alert" announces the error when it appears. -->
          <div role="alert" class="field-error-region">
            @if (formatError()) {
              <p class="field-error">{{ 'voter.verify.format' | translate }}</p>
            }
          </div>
          <ion-button type="submit" expand="block" color="secondary" [disabled]="busy()">
            @if (busy()) {
              <ion-spinner name="crescent" aria-hidden="true"></ion-spinner>
            } @else {
              {{ 'voter.verify.check' | translate }}
            }
          </ion-button>
        </form>
        <div aria-live="polite">
          @switch (result()) {
            @case ('present') {
              <p class="result ok"><ion-icon name="checkmark-circle-outline" aria-hidden="true"></ion-icon>
                {{ 'voter.verify.present' | translate }}</p>
            }
            @case ('absent') {
              <p class="result ko"><ion-icon name="close-circle-outline" aria-hidden="true"></ion-icon>
                {{ 'voter.verify.absent' | translate }}</p>
            }
            @case ('unknownProcess') {
              <p class="result ko">{{ 'voter.verify.unknownProcess' | translate }}</p>
            }
            @case ('error') {
              <p class="result ko">{{ 'voter.verify.error' | translate }}</p>
            }
          }
        </div>
        <ion-text color="medium"><p class="note">{{ 'voter.verify.limits' | translate }}</p></ion-text>
      </main>
    </ion-content>
    <ion-footer class="ion-padding">
      <ion-text color="medium"><p class="secure">{{ 'voter.footer.secure' | translate }}</p></ion-text>
    </ion-footer>
  `,
  styles: `
    .voter { max-width: 35rem; margin: 0 auto; }
    ion-input { margin: 1rem 0 0.75rem; font-size: 1rem; }
    ion-input.field-invalid { --border-color: var(--ion-color-danger); --highlight-color: var(--ion-color-danger); }
    .field-error { color: var(--ion-color-danger); font-size: 0.875rem; font-weight: 600; margin: 0 0 0.75rem; }
    ion-button { --min-height: 52px; }
    .result { display: flex; gap: 8px; align-items: center; padding: 12px; border-radius: var(--vv-radius-control);
      margin-top: 1rem; font-weight: 600; }
    .result ion-icon { font-size: 1.5rem; flex: none; }
    .ok { background: #e6f4ee; color: var(--ion-color-success); }
    .ko { background: #fce9e7; color: var(--ion-color-danger); }
    .note { font-size: 0.875rem; margin-top: 1.5rem; }
    .secure { text-align: center; margin: 0; font-size: 0.875rem; }
  `,
})
export class ReceiptCheckPage {
  readonly processId = input.required<string>();

  private readonly api = inject(VoterApiService);
  protected readonly code = signal<string>(
    (inject(Router).currentNavigation()?.extras.state?.['receiptCode'] as string | undefined)
      ?? (history.state?.receiptCode as string | undefined) ?? '',
  );
  protected readonly busy = signal(false);
  protected readonly formatError = signal(false);
  protected readonly result = signal<Result>(null);

  protected async check(): Promise<void> {
    const code = this.code().trim().toLowerCase();
    if (!/^[0-9a-f]{64}$/.test(code)) {
      this.formatError.set(true);
      return;
    }
    const processId = Number(this.processId());
    this.busy.set(true);
    try {
      this.result.set((await this.api.receiptPresent(processId, code)) ? 'present' : 'absent');
    } catch (error) {
      this.result.set(error instanceof VoterApiError && error.status === 404 ? 'unknownProcess' : 'error');
    } finally {
      this.busy.set(false);
    }
  }
}
