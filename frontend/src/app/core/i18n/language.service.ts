import { DOCUMENT, Injectable, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { APP_CONFIG, AppLanguage } from '../config/app-config';

const STORAGE_KEY = 'verivoto.language';

/**
 * Interface language (RNF-5). Switching only reloads the texts: form state lives in the components, so it is
 * not lost (RF-20). The choice is remembered in this browser.
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);
  private readonly document = inject(DOCUMENT);

  readonly current = signal<AppLanguage>(APP_CONFIG.defaultLanguage);

  init(): void {
    this.use(this.stored() ?? this.fromBrowser() ?? APP_CONFIG.defaultLanguage);
  }

  use(language: AppLanguage): void {
    this.translate.use(language);
    this.current.set(language);
    this.document.documentElement.lang = language;
    try {
      localStorage.setItem(STORAGE_KEY, language);
    } catch {
      // Storage may be unavailable (private mode): the choice simply is not remembered.
    }
  }

  private stored(): AppLanguage | null {
    try {
      return this.valid(localStorage.getItem(STORAGE_KEY));
    } catch {
      return null;
    }
  }

  private fromBrowser(): AppLanguage | null {
    return this.valid(navigator.language?.slice(0, 2) ?? null);
  }

  private valid(value: string | null): AppLanguage | null {
    return (APP_CONFIG.languages as readonly string[]).includes(value ?? '') ? (value as AppLanguage) : null;
  }
}
