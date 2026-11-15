import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { TextTranslationService, TRANSLATE_URL } from './text-translation.service';

describe('TextTranslationService (LibreTranslate, D-62)', () => {
  let service: TextTranslationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TextTranslationService);
    http = TestBed.inject(HttpTestingController);
  });

  it('sends every text in one request and returns them in order', async () => {
    const result = service.translate(['Hola', 'Adiós'], 'es', 'en');
    const req = http.expectOne(TRANSLATE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ q: ['Hola', 'Adiós'], source: 'es', target: 'en', format: 'text' });
    req.flush({ translatedText: ['Hello ', 'Goodbye'] });
    expect(await result).toEqual(['Hello', 'Goodbye']);
  });

  it('returns null when the translator is not running', async () => {
    const result = service.translate(['Hola'], 'es', 'en');
    http.expectOne(TRANSLATE_URL).flush('Bad Gateway', { status: 502, statusText: 'Bad Gateway' });
    expect(await result).toBeNull();
  });

  it('returns null for an answer that does not match the texts sent', async () => {
    const result = service.translate(['Hola', 'Adiós'], 'es', 'en');
    http.expectOne(TRANSLATE_URL).flush({ translatedText: ['Hello'] });
    expect(await result).toBeNull();
  });

  it('does not call the translator without texts', async () => {
    expect(await service.translate([], 'es', 'en')).toEqual([]);
    http.expectNone(TRANSLATE_URL);
  });
});
