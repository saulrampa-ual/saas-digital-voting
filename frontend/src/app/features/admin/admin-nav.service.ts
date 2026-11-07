import { Injectable, signal } from '@angular/core';

/** Expanded/collapsed state of the console side navigation (icons only when collapsed). */
@Injectable({ providedIn: 'root' })
export class AdminNavService {
  readonly expanded = signal(false);

  toggle(): void {
    this.expanded.update((value) => !value);
  }

  close(): void {
    this.expanded.set(false);
  }
}
