import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonButton, IonIcon, IonItem, IonLabel, IonList, IonNote, IonSpinner, IonText } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { ProcessApiService, ProcessSummary } from '../../../core/admin/process-api.service';
import { LocalizedPipe } from '../../../shared/pipes/localized.pipe';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { StatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { AdminPageComponent } from '../admin-page.component';

/** Processes of the organization, newest opening first (RF-06, RF-07; entry to A3 and A4). */
@Component({
  selector: 'app-processes',
  imports: [
    AdminPageComponent, RouterLink, IonList, IonItem, IonLabel, IonButton, IonIcon, IonNote, IonSpinner, IonText,
    TranslatePipe, LocalizedPipe, LocalDatePipe, StatusBadgeComponent,
  ],
  template: `
    <app-admin-page titleKey="admin.processes.title">
      <div class="toolbar">
        <p class="intro">{{ 'admin.processes.intro' | translate }}</p>
        @if (canManage) {
          <ion-button color="secondary" routerLink="/admin/procesos/nuevo">
            <ion-icon slot="start" name="add-outline" aria-hidden="true"></ion-icon>
            {{ 'admin.processes.create' | translate }}
          </ion-button>
        }
      </div>
      @if (loading()) {
        <ion-spinner role="status" [attr.aria-label]="'common.loading' | translate" />
      } @else if (error()) {
        <ion-text color="danger"><p role="alert">{{ 'common.loadError' | translate }}</p></ion-text>
      } @else {
        <ion-list class="cards" lines="none">
          @for (process of processes(); track process.id) {
            <ion-item button detail [routerLink]="['/admin/procesos', process.id]">
              <ion-label>
                <h2>{{ process.title | localized: language() }}</h2>
                <p>
                  {{ 'admin.processes.opens' | translate: { date: (process.opensAt | localDate: language()) } }} ·
                  {{ 'admin.processes.closes' | translate: { date: (process.closesAt | localDate: language()) } }}
                </p>
              </ion-label>
              <app-status-badge slot="end" [status]="process.status" />
            </ion-item>
          } @empty {
            <ion-item><ion-note>{{ 'admin.processes.empty' | translate }}</ion-note></ion-item>
          }
        </ion-list>
      }
    </app-admin-page>
  `,
  styles: `
    .toolbar { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
    .intro { margin: 0; color: var(--ion-color-medium); }
    .cards ion-item { --background: #fff; border: 1px solid var(--vv-border-color); border-radius: var(--vv-radius-card);
      margin-bottom: 12px; --min-height: 72px; }
    h2 { font-weight: 600; }
  `,
})
export class ProcessesPage {
  private readonly api = inject(ProcessApiService);
  protected readonly language = inject(LanguageService).current;
  protected readonly canManage = inject(AuthService).hasAnyRole(['ADMIN']);
  protected readonly processes = signal<ProcessSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);

  /** Ionic keeps the page alive in the stack: reload whenever it is shown again (after creating a process). */
  ionViewWillEnter(): void {
    this.loading.set(this.processes().length === 0);
    this.api.list()
      .then((processes) => this.processes.set(processes))
      .catch(() => this.error.set(true))
      .finally(() => this.loading.set(false));
  }
}
