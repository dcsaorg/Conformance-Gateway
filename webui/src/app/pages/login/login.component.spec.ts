import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { MessageDialog } from '../../dialogs/message/message-dialog.component';
import {
  AccessDeniedComponent,
  AuthenticationErrorComponent,
} from '../access-denied/access-denied.component';
import { LoginComponent } from './login.component';

describe('Authentication pages', () => {
  let auth: { usesRedirectLogin: boolean; isAuthenticated: jest.Mock; login: jest.Mock };

  beforeEach(async () => {
    auth = {
      usesRedirectLogin: true,
      isAuthenticated: jest.fn(async () => false),
      login: jest.fn(async () => {}),
    };
    await TestBed.configureTestingModule({
      declarations: [LoginComponent],
      imports: [FormsModule, AccessDeniedComponent, AuthenticationErrorComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: MatDialog, useValue: {} },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  });
  afterEach(() => jest.restoreAllMocks());

  it('renders Universal Login without application-owned credentials for Auth0', async () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('input')).toBeNull();
    const button = fixture.nativeElement.querySelector('button');
    expect(button.textContent).toContain('Sign in with DCSA');
    button.click();
    expect(auth.login).toHaveBeenCalledWith();
  });

  it('offers email-only local development login through DummyAuth', async () => {
    auth.usesRedirectLogin = false;
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#login_email')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('input[type="password"]')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Sign in with DCSA');
    const email = fixture.nativeElement.querySelector('#login_email');
    email.value = 'local@example.com';
    email.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    fixture.nativeElement
      .querySelector('form')
      .dispatchEvent(new Event('submit', { cancelable: true }));
    expect(auth.login).toHaveBeenCalledWith('local@example.com');
  });

  it('shows a generic retryable error rather than provider details', async () => {
    auth.login.mockRejectedValue(new Error('sensitive provider details'));
    const dialog = jest.spyOn(MessageDialog, 'open').mockResolvedValue(undefined);
    const fixture = TestBed.createComponent(LoginComponent);
    await fixture.componentInstance.onUniversalLogin();
    expect(fixture.componentInstance.isLoading).toBe(false);
    expect(dialog).toHaveBeenCalledWith(
      {},
      'Login failed',
      'Unable to sign in. Please retry or contact DCSA support.',
    );
  });

  it('explains missing permission without initiating another login', () => {
    const fixture = TestBed.createComponent(AccessDeniedComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      'You do not have access to the Conformance Tool.',
    );
    expect(fixture.nativeElement.textContent).toContain('Please contact DCSA support');
    expect(auth.login).not.toHaveBeenCalled();
  });

  it('offers a workspace retry after reconciliation or provider errors', () => {
    const fixture = TestBed.createComponent(AuthenticationErrorComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/environment');
    expect(fixture.nativeElement.textContent).toContain(
      'Your existing sandboxes and reports have not been changed.',
    );
  });
});
