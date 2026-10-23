import { Injectable, Injector } from '@angular/core';
import { Observable } from 'rxjs';
import { authenticationConfig } from './auth-config';
import { Auth0Auth } from './auth0-auth';
import { AuthInterface } from './auth-interface';
import { DummyAuth } from './dummy-auth';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly authenticator: AuthInterface;
  private initialization?: Promise<boolean>;
  readonly usesRedirectLogin = authenticationConfig.provider === 'auth0';

  constructor(injector: Injector) {
    switch (authenticationConfig.provider) {
      case 'auth0':
        this.authenticator = injector.get(Auth0Auth);
        break;
      case 'dummy':
        this.authenticator = new DummyAuth();
        break;
      default:
        throw new Error('Unsupported authentication provider');
    }
  }

  getAuthStatusObservable(): Observable<boolean> {
    return this.authenticator.getAuthStatusObservable();
  }

  async login(localEmail?: string): Promise<void> {
    return this.authenticator.login(localEmail);
  }

  async logout(): Promise<void> {
    return this.authenticator.logout();
  }

  async isAuthenticated(): Promise<boolean> {
    await this.initializeAuthentication();
    return this.authenticator.isAuthenticated();
  }

  async getApiAuthorization(): Promise<string | null> {
    return this.authenticator.getApiAuthorization();
  }

  async getUserEmail(): Promise<string | null> {
    return this.authenticator.getUserEmail();
  }

  async initializeAuthentication(): Promise<boolean> {
    return (this.initialization ??= this.authenticator.initializeAuthentication());
  }
}
