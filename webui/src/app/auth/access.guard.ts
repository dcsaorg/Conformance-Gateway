import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { ApiService } from '../service/api.service';
import { safeReturnPath } from './auth0-auth';

export const signedInGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return await auth.isAuthenticated() || router.createUrlTree(['/login']);
};

export const accessGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const api = inject(ApiService);
  const router = inject(Router);
  try {
    if (!await auth.isAuthenticated()) {
      sessionStorage.setItem('conformance.returnTo', safeReturnPath(state.url));
      return router.createUrlTree(['/login']);
    }
    await api.checkAccess();
    return true;
  } catch (error: any) {
    if (error.status === 401) {
      await auth.logout();
      return router.createUrlTree(['/login']);
    }
    return router.createUrlTree([error.status === 403
      ? '/access-denied'
      : !await auth.isAuthenticated() ? '/login' : '/auth-error']);
  }
};

