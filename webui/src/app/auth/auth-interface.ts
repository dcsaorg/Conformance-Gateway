import { Observable } from "rxjs";

export interface AuthInterface {

  getAuthStatusObservable(): Observable<boolean>;


  login(username: string, password: string): Promise<void>;

  logout(): Promise<void>;

  isAuthenticated(): Promise<boolean>;

  getUserIdToken(): Promise<string | null>;

  getUserEmail(): Promise<string | null>;

  initializeAuthentication(): Promise<boolean>;
}
