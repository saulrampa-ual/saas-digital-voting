import { Pipe, PipeTransform } from '@angular/core';
import { LocalizedText } from '../../core/voter/voter-api.service';

/**
 * Text in the current language, or in the process default language when there is no translation (RI-5).
 * Pure: the language is an argument, so the view updates when it changes.
 */
@Pipe({ name: 'localized' })
export class LocalizedPipe implements PipeTransform {
  transform(text: LocalizedText | null | undefined, language: string, fallback: string = 'es'): string {
    if (!text) {
      return '';
    }
    const values = text as Record<string, string | undefined>;
    return values[language] ?? values[fallback] ?? Object.values(values).find((value) => !!value) ?? '';
  }
}
