import { Component, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../core/i18n/language.service';
import { AppLanguage } from '../../core/config/app-config';

/**
 * ES/EN selector of the headers (RNF-5), with the flags of the login screen (Keycloak theme, js/verivoto.js).
 * Two toggle buttons (aria-pressed) of at least 44 px. Accessible name = visible code + hidden language name
 * ("ES Español"), so the visible text stays inside the name (WCAG 2.5.3); the flags are decorative and the selected
 * language is also marked with a border, not only with color.
 */
@Component({
  selector: 'app-language-switcher',
  imports: [TranslatePipe],
  template: `
    <div class="langs" role="group" [attr.aria-label]="'language.label' | translate">
      <button type="button" class="lang" [class.on]="language.current() === 'es'"
              [attr.aria-pressed]="language.current() === 'es'" (click)="change('es')">
        <svg viewBox="0 0 22 16" aria-hidden="true" focusable="false">
          <rect width="22" height="16" fill="#C60B1E" /><rect y="4" width="22" height="8" fill="#FFC400" />
        </svg>
        ES<span class="sr-only"> {{ 'language.es' | translate }}</span>
      </button>
      <button type="button" class="lang" [class.on]="language.current() === 'en'"
              [attr.aria-pressed]="language.current() === 'en'" (click)="change('en')">
        <svg viewBox="0 0 60 40" aria-hidden="true" focusable="false">
          <rect width="60" height="40" fill="#012169" />
          <path d="M0 0L60 40M60 0L0 40" stroke="#fff" stroke-width="8" />
          <path d="M0 0L60 40M60 0L0 40" stroke="#C8102E" stroke-width="3" />
          <path d="M30 0V40M0 20H60" stroke="#fff" stroke-width="13" />
          <path d="M30 0V40M0 20H60" stroke="#C8102E" stroke-width="8" />
        </svg>
        EN<span class="sr-only"> {{ 'language.en' | translate }}</span>
      </button>
    </div>
  `,
  styles: `
    :host { display: inline-flex; }
    .langs { display: flex; gap: 4px; }
    .lang {
      display: inline-flex; align-items: center; gap: 6px; min-height: 44px; min-width: 44px; padding: 0 10px;
      border: 2px solid transparent; border-radius: 8px; cursor: pointer;
      background: color-mix(in srgb, currentColor 10%, transparent); color: inherit;
      font: inherit; font-size: 0.875rem; font-weight: 700;
    }
    .lang:hover { background: color-mix(in srgb, currentColor 18%, transparent); }
    .lang.on { border-color: currentColor; background: transparent; }
    .lang:focus-visible { outline: 3px solid currentColor; outline-offset: 2px; }
    .lang svg { width: 22px; height: 15px; flex: none; border-radius: 2px; box-shadow: 0 0 0 1px #0000001f; }
    .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
    /* Phones: the code only (still 44 px), so the buttons do not crowd or cut the page title */
    @media (max-width: 575.98px) {
      .langs { gap: 2px; margin-inline-start: 8px; }
      .lang { padding: 0 6px; font-size: 0.8125rem; border-width: 1.5px; }
      .lang svg { display: none; }
    }
  `,
})
export class LanguageSwitcherComponent {
  protected readonly language = inject(LanguageService);

  protected change(value: AppLanguage): void {
    this.language.use(value);
  }
}
