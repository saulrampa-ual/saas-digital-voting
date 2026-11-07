import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  IonButton, IonIcon, IonInput, IonItem, IonLabel, IonList, IonNote, IonSpinner, IonText,
} from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';
import { ApiError } from '../../../core/admin/api-error';
import { Census, CensusApiService } from '../../../core/admin/census-api.service';
import { AdminPageComponent } from '../admin-page.component';

/** Censuses of the organization (RF-03): list for ADMIN and CENSUS_MANAGER; only the census manager creates one. */
@Component({
  selector: 'app-censuses',
  imports: [
    AdminPageComponent, RouterLink, IonButton, IonIcon, IonInput, IonItem, IonLabel, IonList, IonNote,
    IonSpinner, IonText, TranslatePipe,
  ],
  template: `
    <app-admin-page titleKey="admin.censuses.title">
      <p class="intro">{{ (canEdit ? 'admin.censuses.introManager' : 'admin.censuses.introAdmin') | translate }}</p>

      @if (canEdit) {
        <form class="create" (submit)="$event.preventDefault(); create()" novalidate>
          <div class="field">
            <ion-input fill="outline" labelPlacement="stacked" [label]="'admin.censuses.newName' | translate"
                       [value]="name()" (ionInput)="name.set($any($event.detail.value) ?? '')" [maxlength]="100" [counter]="true"
                       [attr.aria-invalid]="!!nameError()" [attr.aria-describedby]="nameError() ? 'census-name-error' : null" />
            @if (nameError(); as message) { <p id="census-name-error" class="field-error" role="alert">{{ message }}</p> }
          </div>
          <ion-button type="submit" color="secondary" [disabled]="creating()">
            <ion-icon slot="start" name="add-outline" aria-hidden="true"></ion-icon>
            {{ 'admin.censuses.create' | translate }}
          </ion-button>
        </form>
      }

      @if (loading()) {
        <ion-spinner role="status" [attr.aria-label]="'common.loading' | translate" />
      } @else if (error()) {
        <ion-text color="danger"><p role="alert">{{ 'common.loadError' | translate }}</p></ion-text>
      } @else {
        <ion-list class="cards" lines="none">
          @for (census of censuses(); track census.id) {
            <ion-item button detail [routerLink]="['/admin/censos', census.id]">
              <ion-label>
                <h2>{{ census.name }}</h2>
                <p>{{ 'admin.censuses.voters' | translate: { count: census.voterCount } }}</p>
              </ion-label>
              <span slot="end" class="badge" [class.frozen]="census.frozen">
                {{ (census.frozen ? 'admin.censuses.frozen' : 'admin.censuses.editable') | translate }}</span>
            </ion-item>
          } @empty {
            <ion-item><ion-note>{{ 'admin.censuses.empty' | translate }}</ion-note></ion-item>
          }
        </ion-list>
      }
    </app-admin-page>
  `,
  styles: `
    .intro { margin: 0 0 1rem; color: var(--ion-color-medium); }
    .create { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-start; margin-bottom: 1.5rem; }
    .create .field { flex: 1 1 280px; max-width: 480px; }
    .create ion-button { margin: 4px 0 0; min-height: 48px; }
    .field-error { color: var(--ion-color-danger); font-size: 0.875rem; margin: 4px 0 0; }
    .cards ion-item { --background: #fff; border: 1px solid var(--vv-border-color); border-radius: var(--vv-radius-card);
      margin-bottom: 12px; --min-height: 72px; }
    h2 { font-weight: 600; }
    .badge { font-size: 0.875rem; font-weight: 600; padding: 2px 10px; border-radius: 999px; border: 1px solid currentColor;
      color: #14573f; background: #e6f4ee; white-space: nowrap; }
    .badge.frozen { color: #7a4b00; background: #fff4dc; }
  `,
})
export class CensusesPage {
  private readonly api = inject(CensusApiService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  protected readonly canEdit = inject(AuthService).hasAnyRole(['CENSUS_MANAGER']);
  protected readonly censuses = signal<Census[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly creating = signal(false);
  protected readonly nameError = signal<string | null>(null);
  protected readonly name = signal('');

  /** Reload whenever the page is shown again (the counts change after importing). */
  ionViewWillEnter(): void {
    this.loading.set(this.censuses().length === 0);
    this.api.list()
      .then((censuses) => this.censuses.set(censuses))
      .catch(() => this.error.set(true))
      .finally(() => this.loading.set(false));
  }

  protected async create(): Promise<void> {
    const name = this.name().trim();
    if (!name) {
      this.nameError.set(this.translate.instant('admin.censuses.nameRequired'));
      return;
    }
    this.nameError.set(null);
    this.creating.set(true);
    try {
      const census = await this.api.create(name);
      this.name.set('');
      await this.router.navigate(['/admin/censos', census.id]);
    } catch (error) {
      this.nameError.set(error instanceof ApiError && error.detail ? error.detail : this.translate.instant('common.loadError'));
    } finally {
      this.creating.set(false);
    }
  }
}
