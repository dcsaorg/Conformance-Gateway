import { Observable, Subject } from "rxjs";
import { AuthInterface } from "./auth-interface";

class DummyAuthStatus {
  constructor(
    public userConfirmed: boolean,
    public isAuthenticated: boolean,
    public userEmail: string,
  ) {}

  save(): void {
    localStorage.setItem('DummyAuthStatus.userConfirmed', this.userConfirmed.toString());
    localStorage.setItem('DummyAuthStatus.isAuthenticated', this.isAuthenticated.toString());
    localStorage.setItem('DummyAuthStatus.userEmail', this.userEmail);
  }

  load(): void {
    this.userConfirmed = (localStorage.getItem('DummyAuthStatus.userConfirmed') || 'false') === 'true';
    this.isAuthenticated = (localStorage.getItem('DummyAuthStatus.isAuthenticated') || 'false') === 'true';
    this.userEmail = localStorage.getItem('DummyAuthStatus.userEmail') || '';
  }
}

export class DummyAuth implements AuthInterface {

  private readonly _authStatus: DummyAuthStatus = new DummyAuthStatus(
    false,
    false,
    ''
  );

  private readonly _authStatusSubject = new Subject<boolean>();

  private emailToVerify: string = '';

  constructor() {
    console.warn('Using DUMMY authenticator!');
  }

  getAuthStatusObservable(): Observable<boolean> {
    return this._authStatusSubject.asObservable();
  }


  async login(username: string, password: string): Promise<void> {
    this._authStatus.userConfirmed = true;
    this._authStatus.isAuthenticated = true;
    this._authStatus.userEmail = username;
    this._authStatus.save();
    this._authStatusSubject.next(this._authStatus.isAuthenticated);
  }

  async logout(): Promise<void> {
    this._authStatus.userConfirmed = false;
    this._authStatus.isAuthenticated = false;
    this._authStatus.userEmail = '';
    this._authStatus.save();
    this._authStatusSubject.next(this._authStatus.isAuthenticated);
  }

  async isAuthenticated(): Promise<boolean> {
    return this._authStatus.isAuthenticated;
  }

  async getUserIdToken(): Promise<string | null> {
    if (!this._authStatus.isAuthenticated) {
      return "";
    }
    const userId: string = this._authStatus.userEmail.split('@').join('-at-').split('.').join('-dot-');
    return `DummyAuthToken#${userId}`;
  }

  async getUserEmail(): Promise<string | null> {
    return this._authStatus.userEmail;
  }

  async initializeAuthentication(): Promise<boolean> {
    this._authStatus.load();
    this._authStatusSubject.next(this._authStatus.isAuthenticated);
    return this._authStatus.isAuthenticated;
  }
}
