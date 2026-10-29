import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { AuthService, ManagementRole } from './auth.service';

/**
 * Requires a logged-in management user with one of the given roles. Without session, Keycloak shows its login
 * (password + email code). With session but without the role, the user is sent to the "no permission" page.
 * Used as `canMatch`, so the code of an area is not downloaded by a role that cannot open it (prototype guide).
 * Guards only hide screens: permissions and tenant isolation are always enforced by the services.
 */
export function roleGuard(allowed: readonly ManagementRole[]): CanMatchFn {
  return async () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!(await auth.ensureLoggedIn())) {
      return false;
    }
    return auth.hasAnyRole(allowed) ? true : router.parseUrl('/admin/sin-permiso');
  };
}
