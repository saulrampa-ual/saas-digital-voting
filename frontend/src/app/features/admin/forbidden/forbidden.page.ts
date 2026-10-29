import { Component } from '@angular/core';
import { ErrorStateComponent } from '../../../shared/error-state/error-state.component';
import { AdminPageComponent } from '../admin-page.component';

@Component({
  selector: 'app-forbidden',
  imports: [AdminPageComponent, ErrorStateComponent],
  template: `
    <app-admin-page titleKey="admin.forbidden.title">
      <app-error-state titleKey="admin.forbidden.title" messageKey="admin.forbidden.message" icon="lock-closed-outline" />
    </app-admin-page>
  `,
})
export class ForbiddenPage {}
