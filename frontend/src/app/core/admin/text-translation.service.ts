import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';
import { Lang } from './process-api.service';

/** Local LibreTranslate behind the dev-server proxy (proxy.conf.json → 127.0.0.1:5000, infra/compose.yaml, D-62). */
export const TRANSLATE_URL = '/translate-api/translate';
const TIMEOUT_MS = 15000;

/**
 * Machine translation es↔en of the texts of a process (A3). It only proposes a text that the person reviews; when the
 * translator is not running or does not answer in time it returns null and the caller keeps the text as written.
 * Nothing but the texts and the two language codes is sent, and only to this machine.
 */
@Injectable({ providedIn: 'root' })
export class TextTranslationService {
  private readonly http = inject(HttpClient);

  async translate(texts: string[], source: Lang, target: Lang): Promise<string[] | null> {
    if (texts.length === 0) return [];
    try {
      const response = await firstValueFrom(
        this.http.post<{ translatedText: string[] }>(TRANSLATE_URL, { q: texts, source, target, format: 'text' })
          .pipe(timeout(TIMEOUT_MS)),
      );
      const out = response?.translatedText;
      return Array.isArray(out) && out.length === texts.length && out.every((t) => typeof t === 'string')
        ? out.map((t) => t.trim())
        : null;
    } catch {
      return null;
    }
  }
}
