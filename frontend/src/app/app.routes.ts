import { Routes } from '@angular/router';
import { roleGuard } from './core/auth/role.guard';

const ANY_MANAGEMENT_ROLE = ['SUPERADMIN', 'ADMIN', 'CENSUS_MANAGER', 'AUDITOR'] as const;

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'admin' },
  // Voter area (RF-08–RF-10): no Keycloak, the personal link and the email code identify the voter.
  {
    path: 'votar/:linkToken',
    loadComponent: () => import('./features/voter/voter-flow.page').then((m) => m.VoterFlowPage),
  },
  {
    path: 'recibo/:processId',
    loadComponent: () => import('./features/voter/receipt-check.page').then((m) => m.ReceiptCheckPage),
  },
  {
    // V8: public result of a PUBLIC_LINK process (D-16); no session.
    path: 'resultados/:publicId',
    loadComponent: () => import('./features/public/public-results.page').then((m) => m.PublicResultsPage),
  },
  {
    path: 'admin',
    canMatch: [roleGuard(ANY_MANAGEMENT_ROLE)],
    loadComponent: () => import('./features/admin/admin-shell.component').then((m) => m.AdminShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        loadComponent: () => import('./features/admin/home/admin-home.page').then((m) => m.AdminHomePage),
      },
      {
        path: 'organizaciones',
        canMatch: [roleGuard(['SUPERADMIN'])],
        loadComponent: () =>
          import('./features/admin/organizations/organizations.page').then((m) => m.OrganizationsPage),
      },
      {
        path: 'censos',
        canMatch: [roleGuard(['ADMIN', 'CENSUS_MANAGER'], { needsOrganization: true })],
        loadComponent: () => import('./features/admin/censuses/censuses.page').then((m) => m.CensusesPage),
      },
      {
        path: 'censos/:id',
        canMatch: [roleGuard(['ADMIN', 'CENSUS_MANAGER'], { needsOrganization: true })],
        loadComponent: () => import('./features/admin/censuses/census-detail.page').then((m) => m.CensusDetailPage),
      },
      {
        path: 'procesos',
        canMatch: [roleGuard(['ADMIN', 'AUDITOR'], { needsOrganization: true })],
        loadComponent: () => import('./features/admin/processes/processes.page').then((m) => m.ProcessesPage),
      },
      {
        path: 'procesos/nuevo',
        canMatch: [roleGuard(['ADMIN'], { needsOrganization: true })],
        loadComponent: () => import('./features/admin/processes/process-form.page').then((m) => m.ProcessFormPage),
      },
      {
        path: 'procesos/:id/editar',
        canMatch: [roleGuard(['ADMIN'], { needsOrganization: true })],
        loadComponent: () => import('./features/admin/processes/process-form.page').then((m) => m.ProcessFormPage),
      },
      {
        path: 'procesos/:id',
        canMatch: [roleGuard(['ADMIN', 'AUDITOR'], { needsOrganization: true })],
        loadComponent: () => import('./features/admin/processes/process-detail.page').then((m) => m.ProcessDetailPage),
      },
      {
        path: 'participacion',
        canMatch: [roleGuard(['ADMIN'], { needsOrganization: true })],
        loadComponent: () => import('./features/admin/participation/participation.page').then((m) => m.ParticipationPage),
      },
      {
        path: 'resultados',
        canMatch: [roleGuard(['ADMIN', 'AUDITOR'], { needsOrganization: true })],
        loadComponent: () => import('./features/admin/results/results.page').then((m) => m.ResultsPage),
      },
      {
        path: 'auditoria',
        canMatch: [roleGuard(['AUDITOR'], { needsOrganization: true })],
        loadComponent: () => import('./features/admin/audit/audit.page').then((m) => m.AuditPage),
      },
      {
        path: 'trazabilidad',
        canMatch: [roleGuard(['ADMIN', 'AUDITOR'], { needsOrganization: true })],
        loadComponent: () => import('./features/admin/traceability/traceability.page').then((m) => m.TraceabilityPage),
      },
      {
        path: 'sin-permiso',
        loadComponent: () => import('./features/admin/forbidden/forbidden.page').then((m) => m.ForbiddenPage),
      },
    ],
  },
  { path: '**', loadComponent: () => import('./features/not-found.page').then((m) => m.NotFoundPage) },
];
