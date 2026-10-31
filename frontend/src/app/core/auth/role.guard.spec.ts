import { TestBed } from '@angular/core/testing';
import { Route, Router, UrlSegment } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  const redirect = { path: '/admin/sin-permiso' };
  const auth = {
    ensureLoggedIn: vi.fn<() => Promise<boolean>>(),
    hasAnyRole: vi.fn<(allowed: readonly ('SUPERADMIN' | 'ADMIN' | 'CENSUS_MANAGER' | 'AUDITOR')[]) => boolean>(),
    organizationId: vi.fn<() => string | null>(),
  };
  const router = { parseUrl: vi.fn(() => redirect) };

  beforeEach(() => {
    vi.resetAllMocks();
    auth.ensureLoggedIn.mockResolvedValue(true);
    auth.hasAnyRole.mockReturnValue(true);
    auth.organizationId.mockReturnValue('org-synthetic');
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    });
  });

  async function run(needsOrganization = false): Promise<unknown> {
    const guard = roleGuard(['ADMIN'], { needsOrganization });
    return TestBed.runInInjectionContext(() => guard({} as Route, [] as UrlSegment[], {} as never));
  }

  it('blocks an unauthenticated management user', async () => {
    auth.ensureLoggedIn.mockResolvedValue(false);

    await expect(run()).resolves.toBe(false);
    expect(auth.hasAnyRole).not.toHaveBeenCalled();
  });

  it('allows the expected role when the route has no organization requirement', async () => {
    await expect(run()).resolves.toBe(true);
  });

  it('redirects a user with an allowed role but no organization when required', async () => {
    auth.organizationId.mockReturnValue(null);

    await expect(run(true)).resolves.toBe(redirect);
    expect(router.parseUrl).toHaveBeenCalledWith('/admin/sin-permiso');
  });

  it('redirects a user whose role is not allowed', async () => {
    auth.hasAnyRole.mockReturnValue(false);

    await expect(run()).resolves.toBe(redirect);
  });
});
