import { Observable } from 'rxjs';

export interface AuthInterface {
  getAuthStatusObservable(): Observable<boolean>;

  /** Start sign-in; local development may supply an email without credentials. */
  login(localEmail?: string): Promise<void>;

  logout(): Promise<void>;

  isAuthenticated(): Promise<boolean>;

  /** Complete Authorization header value for the application's API. */
  getApiAuthorization(): Promise<string | null>;

  getUserEmail(): Promise<string | null>;

  initializeAuthentication(): Promise<boolean>;
}
