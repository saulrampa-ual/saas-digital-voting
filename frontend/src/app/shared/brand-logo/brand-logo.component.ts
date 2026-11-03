import { Component, input } from '@angular/core';

/**
 * VeriVoto logo for dark backgrounds: shield with a check mark and, optionally, the "VeriVoto" wordmark.
 * Same drawing as the prototype and the Keycloak theme. The shield is decorative; the accessible name is the
 * wordmark (visually hidden when it is not shown).
 */
@Component({
  selector: 'app-brand-logo',
  template: `
    <svg class="shield" viewBox="0 0 48 56" aria-hidden="true" focusable="false">
      <path class="outline" d="M24 3 L43 10 V27 C43 40 34 49 24 53 C14 49 5 40 5 27 V10 Z" />
      <path class="outline" d="M16 15 H32" />
      <path class="check" d="M15 31 L22 38 L34 23" />
    </svg>
    <span class="wordmark" [class.sr-only]="!showName()">Veri<span class="accent">Voto</span></span>
  `,
  styles: `
    :host { display: inline-flex; align-items: center; gap: 10px; color: var(--ion-color-tertiary-contrast); }
    .shield { width: 30px; height: 35px; flex: none; }
    .outline, .check { fill: none; stroke-linecap: round; stroke-linejoin: round; }
    .outline { stroke: currentColor; stroke-width: 3.5; }
    .check { stroke: var(--vv-brand-accent); stroke-width: 3.5; }
    .wordmark { font-size: 1.375rem; font-weight: 800; letter-spacing: -0.01em; white-space: nowrap; }
    .accent { color: var(--vv-brand-accent); }
    /* class="name-wide": the name is only shown from 992 px (it stays as the accessible name below). */
    @media (max-width: 991.98px) {
      :host(.name-wide) .wordmark { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
    }
    .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
  `,
})
export class BrandLogoComponent {
  readonly showName = input(true);
}
