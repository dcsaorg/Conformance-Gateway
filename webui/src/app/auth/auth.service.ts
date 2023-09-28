import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { environment } from "src/environments/environment";
import { AuthInterface } from "./auth-interface";
import { CognitoAuth } from "./cognito-auth";
import { DummyAuth } from "./dummy-auth";

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly authenticator: AuthInterface = (
    environment.useCognito
    ? new CognitoAuth()
    : new DummyAuth()
  );

  constructor() {}

  getAuthStatusObservable(): Observable<boolean> {
    return this.authenticator.getAuthStatusObservable();
  }


  async login(username: string, password: string): Promise<void> {
    return this.authenticator.login(username, password);
  }

  async logout(): Promise<void> {
    return this.authenticator.logout();
  }

  async isAuthenticated(): Promise<boolean> {
    return this.authenticator.isAuthenticated();
  }

  async getUserIdToken(): Promise<string | null> {
    return this.authenticator.getUserIdToken();
  }

  async getUserEmail(): Promise<string | null> {
    return this.authenticator.getUserEmail();
  }

  async initializeAuthentication(): Promise<boolean> {
    return this.authenticator.initializeAuthentication();
  }
}
