import { Component, ElementRef, OnInit, computed, effect, inject, input, signal, viewChild } from '@angular/core';
import {
  IonButton, IonButtons, IonContent, IonFooter, IonHeader, IonSpinner, IonText, IonTitle, IonToolbar,
} from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  Ballot, CodeSent, LinkStatus, ProcessStatus, Selection, VoterApiError, VoterApiService,
} from '../../core/voter/voter-api.service';
import { LanguageService } from '../../core/i18n/language.service';
import { LanguageSwitcherComponent } from '../../shared/language-switcher/language-switcher.component';
import { ErrorStateComponent } from '../../shared/error-state/error-state.component';
import { LocalizedPipe } from '../../shared/pipes/localized.pipe';
import { LocalDatePipe } from '../../shared/pipes/local-date.pipe';
import { StepIndicatorComponent } from './steps/step-indicator.component';
import { AccessCodeStepComponent } from './steps/access-code-step.component';
import { BallotChoice, BallotStepComponent } from './steps/ballot-step.component';
import { ReviewStepComponent } from './steps/review-step.component';
import { ReceiptStepComponent } from './steps/receipt-step.component';

/** Screens of the voter flow (prototypes V1–V7, B1–B4, C2–C4). */
type Screen =
  | 'loading' | 'welcome' | 'code' | 'ballot' | 'review' | 'receipt'
  | 'notOpen' | 'closed' | 'invalidLink' | 'expiredLink' | 'locked'
  | 'alreadyVoted' | 'sessionExpired' | 'notRecorded' | 'error';

/**
 * Voter flow from the personal link /votar/:linkToken (RF-08, RF-09, RF-10). One page with an internal state
 * machine: the link, the code and the voting token stay in memory only and never appear in the URL or in storage.
 * Once the vote is recorded, going back cannot reach the ballot again.
 */
@Component({
  selector: 'app-voter-flow',
  imports: [
    IonHeader, IonToolbar, IonTitle, IonButtons, IonContent, IonFooter, IonText, IonButton, IonSpinner,
    TranslatePipe, LanguageSwitcherComponent, ErrorStateComponent, LocalizedPipe, LocalDatePipe,
    StepIndicatorComponent, AccessCodeStepComponent, BallotStepComponent, ReviewStepComponent, ReceiptStepComponent,
  ],
  templateUrl: './voter-flow.page.html',
  styleUrl: './voter-flow.page.scss',
})
export class VoterFlowPage implements OnInit {
  /** Bound from the route (withComponentInputBinding). Never logged or shown. */
  readonly linkToken = input.required<string>();

  private readonly api = inject(VoterApiService);
  private readonly translate = inject(TranslateService);
  protected readonly language = inject(LanguageService).current;
  private readonly main = viewChild<ElementRef<HTMLElement>>('main');
  private readonly codeStep = viewChild(AccessCodeStepComponent);

  protected readonly screen = signal<Screen>('loading');
  protected readonly link = signal<LinkStatus | null>(null);
  protected readonly codeSent = signal<CodeSent | null>(null);
  protected readonly codeError = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly ballot = signal<Ballot | null>(null);
  protected readonly choice = signal<BallotChoice>(null);
  protected readonly receiptCode = signal<string | null>(null);
  protected readonly votedOn = signal<string | null>(null);
  protected readonly lockedUntil = signal<string | null>(null);
  private votingToken: string | null = null;

  protected readonly fallback = computed(() => this.link()?.defaultLanguage ?? 'es');
  protected readonly step = computed(() => {
    switch (this.screen()) {
      case 'code': return 1;
      case 'ballot': return 2;
      case 'review': case 'notRecorded': return 3;
      case 'receipt': return 4;
      default: return 0;
    }
  });
  protected readonly showStatusBar = computed(() =>
    ['welcome', 'code', 'ballot', 'review', 'notRecorded'].includes(this.screen()));
  protected readonly choiceText = computed(() => {
    const choice = this.choice();
    if (choice === 'blank') {
      return this.translate.instant('voter.ballot.blank') as string;
    }
    const option = this.ballot()?.options.find((candidate) => candidate.id === choice);
    const text = option?.text as Record<string, string | undefined> | undefined;
    return text?.[this.language()] ?? text?.[this.fallback()] ?? '';
  });

  constructor() {
    // Each new screen moves the focus to its heading (WCAG 2.4.3), as navigation does between pages.
    effect(() => {
      this.screen();
      setTimeout(() => this.main()?.nativeElement.querySelector<HTMLElement>('h1')?.focus({ preventScroll: true }), 50);
    });
  }

  async ngOnInit(): Promise<void> {
    try {
      const link = await this.api.linkStatus(this.linkToken());
      this.link.set(link);
      if (!this.showProcessState(link.status, link.closesAt)) {
        this.screen.set('welcome');
      }
    } catch (error) {
      this.handleLinkError(error);
    }
  }

  /** V1 «Empezar»: goes to B1 and sends the first code right away. */
  protected start(): void {
    this.screen.set('code');
    void this.sendCode();
  }

  protected async sendCode(): Promise<void> {
    this.busy.set(true);
    this.codeError.set(null);
    try {
      this.codeSent.set(await this.api.sendCode(this.linkToken()));
      this.codeStep()?.clear();
    } catch (error) {
      if (!(error instanceof VoterApiError)) {
        throw error;
      }
      if (error.status === 429) {
        this.codeError.set(this.translate.instant('voter.code.tooSoon'));
      } else if (error.status === 503 || error.status === 0) {
        this.codeError.set(this.translate.instant('voter.code.mailFailed'));
      } else {
        this.handleAccessError(error);
      }
    } finally {
      this.busy.set(false);
    }
  }

  protected async verifyCode(code: string): Promise<void> {
    this.busy.set(true);
    this.codeError.set(null);
    try {
      const granted = await this.api.verifyCode(this.linkToken(), code);
      if (granted.hasVoted) {
        this.votedOn.set(granted.votedOn);
        this.screen.set('alreadyVoted');
        return;
      }
      this.votingToken = granted.votingToken;
      this.ballot.set(await this.api.ballot(this.link()!.processId));
      this.screen.set('ballot');
    } catch (error) {
      if (!(error instanceof VoterApiError)) {
        throw error;
      }
      this.codeStep()?.clear();
      if (error.status === 400) {
        const remaining = error.problem['remainingAttempts'];
        this.codeError.set(this.translate.instant('voter.code.wrong', { remaining }));
      } else if (error.status === 410) {
        this.codeError.set(this.translate.instant('voter.code.expired'));
      } else {
        this.handleAccessError(error);
      }
    } finally {
      this.busy.set(false);
    }
  }

  protected review(): void {
    this.screen.set('review');
  }

  protected changeSelection(): void {
    this.screen.set('ballot');
  }

  /** B3 «Confirmar y votar» and V7 «Reintentar»: the same token and selection, so a retry never votes twice. */
  protected async confirm(): Promise<void> {
    const choice = this.choice();
    if (this.busy() || choice === null || !this.votingToken) {
      return;
    }
    const selection: Selection = choice === 'blank' ? { blank: true } : { optionId: choice };
    this.busy.set(true);
    try {
      const result = await this.api.castVote(this.link()!.processId, this.votingToken, selection);
      this.votingToken = null;
      this.receiptCode.set(result.receiptCode);
      this.screen.set('receipt');
    } catch (error) {
      if (!(error instanceof VoterApiError)) {
        throw error;
      }
      if (error.status === 409) {
        this.votingToken = null;
        this.screen.set('alreadyVoted');
      } else if (error.status === 404 || error.status === 410) {
        this.votingToken = null;
        this.screen.set('sessionExpired');
      } else if (error.status === 422) {
        this.votingToken = null;
        this.screen.set('closed');
      } else if (error.status === 400) {
        this.screen.set('ballot');
      } else {
        this.screen.set('notRecorded');
      }
    } finally {
      this.busy.set(false);
    }
  }

  /** V6 and C4: back to the code step with the same personal link. */
  protected enterAgain(): void {
    this.votingToken = null;
    this.choice.set(null);
    this.codeSent.set(null);
    this.codeError.set(null);
    this.screen.set('code');
  }

  private showProcessState(status: ProcessStatus, closesAt: string): Screen | null {
    if (status === 'SCHEDULED') {
      this.screen.set('notOpen');
      return 'notOpen';
    }
    if (status !== 'OPEN' || Date.parse(closesAt) <= Date.now()) {
      this.screen.set('closed');
      return 'closed';
    }
    return null;
  }

  private handleLinkError(error: unknown): void {
    if (error instanceof VoterApiError && error.status === 404) {
      this.screen.set('invalidLink');
    } else if (error instanceof VoterApiError && error.status === 410) {
      this.screen.set('expiredLink');
    } else {
      this.screen.set('error');
    }
  }

  private handleAccessError(error: VoterApiError): void {
    if (error.status === 423) {
      this.lockedUntil.set((error.problem['lockedUntil'] as string | undefined) ?? null);
      this.screen.set('locked');
    } else if (error.status === 422) {
      this.screen.set(error.processStatus === 'SCHEDULED' ? 'notOpen' : 'closed');
    } else {
      this.handleLinkError(error);
    }
  }
}
