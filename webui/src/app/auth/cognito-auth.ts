import {
  AuthenticationDetails,
  CognitoUser,
  CognitoUserAttribute,
  CognitoUserPool,
  CognitoUserSession
} from "amazon-cognito-identity-js";
import { Observable, Subject } from "rxjs";
import { environment } from "src/environments/environment";
import { AuthInterface } from "./auth-interface";

export class CognitoAuth implements AuthInterface {

  private readonly cognitoUserPool: CognitoUserPool = new CognitoUserPool({
    UserPoolId: environment.cognitoUserPoolId,
    ClientId: environment.cognitoClientId,
  });

  private authStatusSubject = new Subject<boolean>();

  getAuthStatusObservable(): Observable<boolean> {
    return this.authStatusSubject.asObservable();
  }


  async login(username: string, password: string): Promise<void> {
    const authService: CognitoAuth = this;
    return new Promise<void>(
      (
        resolve: (value: void | PromiseLike<void>) => void,
        reject: (reason?: any) => void
      ) => {
        try {
          const cognitoUser: CognitoUser = new CognitoUser({
            Username: username,
            Pool: this.cognitoUserPool,
          });
          const authDetails: AuthenticationDetails = new AuthenticationDetails({
            Username: username,
            Password: password,
          });
          cognitoUser.authenticateUser(
            authDetails,
            {
              onSuccess(session: CognitoUserSession): void {
                authService.authStatusSubject.next(true);
                resolve();
              },
              onFailure(error: any): void {
                reject(error);
              }
            }
          );
        } catch (error) {
          reject(error);
        }
      }
    );
  }

  async logout(): Promise<void> {
    this.authStatusSubject.next(false);
    const cognitoUser: CognitoUser | null = this.cognitoUserPool.getCurrentUser();
    if (!cognitoUser) {
      return;
    }
    return new Promise<void>(
      (
        resolve: (value: void | PromiseLike<void>) => void,
        reject: (reason?: any) => void
      ) => {
        try {
          cognitoUser.signOut(
            () => {
              resolve();
            }
          );
        } catch (error) {
          reject(error);
        }
      }
    );
  }

  private async _getUserSession(): Promise<CognitoUserSession | null> {
    const cognitoUser: CognitoUser | null = this.cognitoUserPool.getCurrentUser();
    if (!cognitoUser) {
      return null;
    }
    return new Promise<CognitoUserSession>(
      (
        resolve: (value: CognitoUserSession | PromiseLike<CognitoUserSession>) => void,
        reject: (reason?: any) => void
      ) => {
        try {
          cognitoUser.getSession(
            (error: Error | null, session: CognitoUserSession | null) => {
              if (error) {
                reject(error);
              } else {
                if (!session) {
                  reject('No error, but missing session');
                } else {
                  resolve(session);
                }
              }
            }
          );
        } catch (error) {
          reject(error);
        }
      }
    );
  }

  async isAuthenticated(): Promise<boolean> {
    const session: CognitoUserSession | null = await this._getUserSession();
    if (!session) {
      this.authStatusSubject.next(false);
      return false;
    }
    const authenticated: boolean = session.isValid();
    this.authStatusSubject.next(authenticated);
    return authenticated;
  }

  async getUserIdToken(): Promise<string | null> {
    const session: CognitoUserSession | null = await this._getUserSession();
    if (!session) {
      this.authStatusSubject.next(false);
      return null;
    }
    if (!session.isValid()) {
      this.authStatusSubject.next(false);
      return null;
    }
    this.authStatusSubject.next(true);
    return session.getIdToken().getJwtToken();
  }

  async getUserEmail(): Promise<string | null> {
    return null;
  };

  async initializeAuthentication(): Promise<boolean> {
    return await this.isAuthenticated();
  }
}
