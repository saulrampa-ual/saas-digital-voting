import { Component, inject } from '@angular/core';
import { IonCard, IonCardHeader, IonCardTitle, IonCardContent } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';
import { AdminPageComponent } from '../admin-page.component';

@Component({
  selector: 'app-admin-home',
  imports: [AdminPageComponent, IonCard, IonCardHeader, IonCardTitle, IonCardContent, TranslatePipe],
  template: `
    <app-admin-page titleKey="admin.home.title">
      <ion-card>
        <ion-card-header>
          <ion-card-title>{{ 'admin.home.welcome' | translate: { name: auth.displayName() } }}</ion-card-title>
        </ion-card-header>
        <ion-card-content>
          <p>
            {{ 'admin.home.roles' | translate }}:
            @for (role of auth.roles(); track role) {<span>{{ 'roles.' + role | translate }}{{ $last ? '' : ', ' }}</span>}
          </p>
          @if (auth.organizationId(); as organization) {
            <p>{{ 'admin.home.organization' | translate }}: {{ organization }}</p>
          }
        </ion-card-content>
      </ion-card>
    </app-admin-page>
  `,
})
export class AdminHomePage {
  protected readonly auth = inject(AuthService);
}
