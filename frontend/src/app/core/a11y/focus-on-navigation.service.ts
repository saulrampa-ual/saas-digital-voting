import { Injectable, inject, DestroyRef, DOCUMENT } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';

/**
 * After every navigation, moves the keyboard and screen reader focus to the `h1` of the visible page
 * (WCAG 2.4.3, prototype recommendations section 1). Ionic keeps previous pages in the DOM, hidden with
 * `.ion-page-hidden`, so the heading is searched only in the pages that are not hidden (innermost first).
 */
@Injectable({ providedIn: 'root' })
export class FocusOnNavigationService {
  private readonly router = inject(Router);
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);
  private firstNavigation = true;

  init(): void {
    const subscription = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        // The first load keeps the browser default (focus at the start of the document).
        if (this.firstNavigation) {
          this.firstNavigation = false;
          return;
        }
        // Wait for Ionic to attach the entering page and finish its transition.
        setTimeout(() => this.focusHeading(), 350);
      });
    this.destroyRef.onDestroy(() => subscription.unsubscribe());
  }

  private focusHeading(): void {
    const pages = Array.from(this.document.querySelectorAll<HTMLElement>('.ion-page:not(.ion-page-hidden)'));
    const heading = pages
      .reverse()
      .map((page) => page.querySelector<HTMLElement>('h1'))
      .find((element): element is HTMLElement => element !== null);
    if (heading) {
      heading.setAttribute('tabindex', '-1');
      heading.focus({ preventScroll: true });
    }
  }
}
