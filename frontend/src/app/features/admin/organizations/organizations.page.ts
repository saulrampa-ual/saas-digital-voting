import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { IonList, IonItem, IonLabel, IonBadge, IonSpinner, IonText } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { APP_CONFIG } from '../../../core/config/app-config';
import { AdminPageComponent } from '../admin-page.component';

interface Organization {
  id: string;
  name: string;
  status: 'ACTIVE' | 'SUSPENDED';
}

interface Page<T> {
  items: T[];
  totalItems: number;
}

/** E2 (RF-01), first version: read-only list to check login and API access end to end. */
@Component({
  selector: 'app-organizations',
  imports: [AdminPageComponent, IonList, IonItem, IonLabel, IonBadge, IonSpinner, IonText, TranslatePipe],
  template: `
    <app-admin-page titleKey="admin.organizations.title">
      @if (loading()) {
        <ion-spinner role="status" [attr.aria-label]="'common.loading' | translate" />
      } @else if (error()) {
        <ion-text color="danger"><p role="alert">{{ 'common.loadError' | translate }}</p></ion-text>
      } @else {
        <ion-list>
          @for (organization of organizations(); track organization.id) {
            <ion-item>
              <ion-label>
                <h2>{{ organization.name }}</h2>
                <p>{{ organization.id }}</p>
              </ion-label>
              <ion-badge slot="end" [color]="organization.status === 'ACTIVE' ? 'success' : 'medium'">
                {{ 'admin.organizations.status.' + organization.status | translate }}
              </ion-badge>
            </ion-item>
          } @empty {
            <ion-item><ion-label>{{ 'admin.organizations.empty' | translate }}</ion-label></ion-item>
          }
        </ion-list>
      }
    </app-admin-page>
  `,
})
export class OrganizationsPage {
  private readonly http = inject(HttpClient);
  protected readonly organizations = signal<Organization[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);

  constructor() {
    this.http.get<Page<Organization>>(`${APP_CONFIG.apiBaseUrl}/api/v1/organizations`).subscribe({
      next: (page) => {
        this.organizations.set(page.items);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}
