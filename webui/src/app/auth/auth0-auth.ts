import { Injectable } from '@angular/core';
import { AuthService as Auth0Sdk } from '@auth0/auth0-angular';
import { filter, firstValueFrom, Observable, switchMap, take } from 'rxjs';
import { AuthInterface } from './auth-interface';

export function safeReturnPath(path: unknown): string {
  return typeof path === 'string' &&
    /^\/(?!\/)/.test(path) &&
    !/[\\\r\n]/.test(path) &&
    !/^\/(login|access-denied|auth-error)(?:[/?#]|$)/.test(path)
    ? path
    : '/environment';
}

@Injectable({ providedIn: 'root' })
export class Auth0Auth implements AuthInterface {
  constructor(private readonly sdk: Auth0Sdk) {}

  getAuthStatusObservable(): Observable<boolean> {
    return this.sdk.isAuthenticated$;
  }

  async login(): Promise<void> {
    const returnTo = safeReturnPath(sessionStorage.getItem('conformance.returnTo'));
    sessionStorage.removeItem('conformance.returnTo');
    await firstValueFrom(this.sdk.loginWithRedirect({ appState: { target: returnTo } }));
  }

  async logout(): Promise<void> {
    // openUrl:false clears SDK tokens/cookies without calling Auth0's SSO logout endpoint.
    await firstValueFrom(this.sdk.logout({ openUrl: false }));
  }

  async isAuthenticated(): Promise<boolean> {
    return firstValueFrom(
      this.sdk.isLoading$.pipe(
        filter((loading) => !loading),
        take(1),
        switchMap(() => this.sdk.isAuthenticated$),
        take(1),
      ),
    );
  }

  async getApiAuthorization(): Promise<string | null> {
    if (!(await this.isAuthenticated())) return null;
    try {
      return 'Bearer ' + (await firstValueFrom(this.sdk.getAccessTokenSilently()));
    } catch {
      await this.logout();
      throw new Error('Your session has ended. Please sign in again.');
    }
  }

  async getUserEmail(): Promise<string | null> {
    return (await firstValueFrom(this.sdk.user$))?.email ?? null;
  }

  initializeAuthentication(): Promise<boolean> {
    return this.isAuthenticated();
  }
}
