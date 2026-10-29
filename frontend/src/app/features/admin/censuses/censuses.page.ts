import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { IonList, IonItem, IonLabel, IonBadge, IonSpinner, IonText, IonNote } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { APP_CONFIG } from '../../../core/config/app-config';
import { AdminPageComponent } from '../admin-page.component';

interface Census {
  id: number;
  name: string;
  voterCount: number;
  frozen: boolean;
}

/** A2 (RF-03), first version: read-only list of censuses of the organization. */
@Component({
  selector: 'app-censuses',
  imports: [AdminPageComponent, IonList, IonItem, IonLabel, IonBadge, IonSpinner, IonText, IonNote, TranslatePipe],
  template: `
    <app-admin-page titleKey="admin.censuses.title">
      @if (loading()) {
        <ion-spinner role="status" [attr.aria-label]="'common.loading' | translate" />
      } @else if (error()) {
        <ion-text color="danger"><p role="alert">{{ 'common.loadError' | translate }}</p></ion-text>
      } @else {
        <ion-list>
          @for (census of censuses(); track census.id) {
            <ion-item>
              <ion-label>
                <h2>{{ census.name }}</h2>
                <p>{{ 'admin.censuses.voters' | translate: { count: census.voterCount } }}</p>
              </ion-label>
              @if (census.frozen) {
                <ion-badge slot="end" color="warning">{{ 'admin.censuses.frozen' | translate }}</ion-badge>
              }
            </ion-item>
          } @empty {
            <ion-item><ion-note>{{ 'admin.censuses.empty' | translate }}</ion-note></ion-item>
          }
        </ion-list>
      }
    </app-admin-page>
  `,
})
export class CensusesPage {
  private readonly http = inject(HttpClient);
  protected readonly censuses = signal<Census[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);

  constructor() {
    this.http.get<Census[]>(`${APP_CONFIG.apiBaseUrl}/api/v1/censuses`).subscribe({
      next: (censuses) => {
        this.censuses.set(censuses);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}
