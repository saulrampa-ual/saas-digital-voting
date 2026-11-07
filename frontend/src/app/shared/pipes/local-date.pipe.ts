import { Pipe, PipeTransform } from '@angular/core';

/**
 * Date (and optionally time) in the voter's language and time zone, with the browser Intl API, so no Angular
 * locale data needs to be registered. Example: «19 de noviembre de 2026, 21:00».
 */
@Pipe({ name: 'localDate' })
export class LocalDatePipe implements PipeTransform {
  transform(value: string | null | undefined, language: string, withTime: boolean = true): string {
    if (!value) {
      return '';
    }
    // A date without time (votedOn, D-18) is a calendar day: format it at noon UTC to avoid day shifts.
    const date = /^\d{4}-\d{2}-\d{2}$/.test(value) ? new Date(`${value}T12:00:00Z`) : new Date(value);
    return new Intl.DateTimeFormat(language, {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      ...(withTime ? { hour: '2-digit', minute: '2-digit' } : {}),
      ...(/^\d{4}-\d{2}-\d{2}$/.test(value) ? { timeZone: 'UTC' } : {}),
    }).format(date);
  }
}
