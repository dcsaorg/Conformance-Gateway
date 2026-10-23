import { BehaviorSubject, of, throwError } from 'rxjs';
import { Auth0Auth, safeReturnPath } from './auth0-auth';
import { AuthService } from './auth.service';
import { authenticationConfig, auth0SdkConfig, validateAuthenticationConfig } from './auth-config';
import { environment } from 'src/environments/environment';

describe('Auth0 integration', () => {
  const original = { ...authenticationConfig };
  afterEach(() => {
    Object.assign(authenticationConfig, original);
    sessionStorage.clear();
    localStorage.clear();
  });

  function fixture() {
    const authenticated = new BehaviorSubject(true);
    const loading = new BehaviorSubject(false);
    const sdk = {
      isAuthenticated$: authenticated,
      isLoading$: loading,
      user$: of({ email: 'alice@example.com' }),
      loginWithRedirect: jest.fn(() => of(undefined)),
      logout: jest.fn(() => {
        authenticated.next(false);
        return of(undefined);
      }),
      getAccessTokenSilently: jest.fn(() => of('api-access-token')),
    };
    return { sdk, authenticated, loading, auth: new Auth0Auth(sdk as any) };
  }

  it('redirects through SDK and preserves a safe deep link', async () => {
    const { auth, sdk } = fixture();
    sessionStorage.setItem('conformance.returnTo', '/sandbox/fixture');
    await auth.login();
    expect(sdk.loginWithRedirect).toHaveBeenCalledWith({
      appState: { target: '/sandbox/fixture' },
    });
    expect(sessionStorage.getItem('conformance.returnTo')).toBeNull();
    for (const unsafe of [
      'https://evil.example/',
      '//evil.example/',
      '/\\evil.example/',
      '/login',
      '/access-denied',
      null,
    ]) {
      expect(safeReturnPath(unsafe)).toBe('/environment');
    }
  });

  it('waits for callback/session restoration before reporting authentication', async () => {
    const { auth, loading, authenticated } = fixture();
    loading.next(true);
    authenticated.next(false);
    let settled = false;
    const result = auth.initializeAuthentication().then((value) => {
      settled = true;
      return value;
    });
    await Promise.resolve();
    expect(settled).toBe(false);
    authenticated.next(true);
    loading.next(false);
    expect(await result).toBe(true);
  });

  it('obtains API access tokens through SDK on every API request, including renewal', async () => {
    const { auth, sdk } = fixture();
    expect(await auth.getApiAuthorization()).toBe('Bearer api-access-token');
    sdk.getAccessTokenSilently.mockReturnValue(of('renewed-access-token'));
    expect(await auth.getApiAuthorization()).toBe('Bearer renewed-access-token');
    expect(await auth.getUserEmail()).toBe('alice@example.com');
    expect(localStorage.length).toBe(0);
  });

  it('logout clears local SDK state without ending central SSO or initiating login', async () => {
    const { auth, sdk } = fixture();
    await auth.logout();
    expect(sdk.logout).toHaveBeenCalledWith({ openUrl: false });
    expect(await auth.isAuthenticated()).toBe(false);
    expect(await auth.getApiAuthorization()).toBeNull();
    expect(sdk.loginWithRedirect).not.toHaveBeenCalled();
  });

  it('renewal failure ends local state without automatically initiating login', async () => {
    const { auth, sdk } = fixture();
    sdk.getAccessTokenSilently.mockReturnValue(throwError(() => new Error('refresh rejected')));
    await expect(auth.getApiAuthorization()).rejects.toThrow('session has ended');
    expect(sdk.logout).toHaveBeenCalledWith({ openUrl: false });
    expect(sdk.loginWithRedirect).not.toHaveBeenCalled();
  });

  it('validates configuration and requests memory-only rotating refresh tokens', () => {
    Object.assign(authenticationConfig, {
      provider: 'auth0',
      issuer: 'https://tenant.example/',
      audience: 'dt-api',
      clientId: 'dt-client',
    });
    const config = auth0SdkConfig();
    expect(() =>
      validateAuthenticationConfig({ ...authenticationConfig, provider: 'cognito' } as any),
    ).toThrow();
    expect(config.cacheLocation).toBe('memory');
    expect(config.useRefreshTokens).toBe(true);
    expect(config.useRefreshTokensFallback).toBe(true);
    expect(config.authorizationParams?.scope).toBe('openid profile email offline_access');
    expect(config.errorPath).toBe('/auth-error');
    expect(() =>
      validateAuthenticationConfig({ ...authenticationConfig, issuer: undefined }),
    ).toThrow();
    expect(() =>
      validateAuthenticationConfig({ ...authenticationConfig, environment: 'other' }),
    ).toThrow();
    expect(() =>
      validateAuthenticationConfig({ ...authenticationConfig, issuer: 'http://tenant.example/' }),
    ).toThrow();
  });

  it('selects Auth0 only explicitly and preserves DummyAuth local session restoration', async () => {
    const { auth } = fixture();
    Object.assign(authenticationConfig, { provider: 'auth0' });
    const injector = { get: jest.fn(() => auth) };
    const service = new AuthService(injector as any);
    expect(service.usesRedirectLogin).toBe(true);
    expect(injector.get).toHaveBeenCalledWith(Auth0Auth);
    Object.assign(authenticationConfig, { provider: 'dummy' });
    expect(environment.name).toBe('local');
    const dummy = new AuthService(injector as any);
    await dummy.login('alice@example.com');
    const restored = new AuthService(injector as any);
    expect(await restored.isAuthenticated()).toBe(true);
    expect(await restored.getApiAuthorization()).toMatch(/^DummyAuthToken#/);
    await restored.logout();
    expect(await restored.isAuthenticated()).toBe(false);
  });
});
