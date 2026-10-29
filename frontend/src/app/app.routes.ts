import { Routes } from '@angular/router';
import { roleGuard } from './core/auth/role.guard';

const ANY_MANAGEMENT_ROLE = ['SUPERADMIN', 'ADMIN', 'CENSUS_MANAGER', 'AUDITOR'] as const;

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'admin' },
  {
    path: 'votar/:linkToken',
    loadComponent: () => import('./features/voter/voter-entry.page').then((m) => m.VoterEntryPage),
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
        canMatch: [roleGuard(['ADMIN', 'CENSUS_MANAGER'])],
        loadComponent: () => import('./features/admin/censuses/censuses.page').then((m) => m.CensusesPage),
      },
      {
        path: 'sin-permiso',
        loadComponent: () => import('./features/admin/forbidden/forbidden.page').then((m) => m.ForbiddenPage),
      },
    ],
  },
  { path: '**', loadComponent: () => import('./features/not-found.page').then((m) => m.NotFoundPage) },
];
