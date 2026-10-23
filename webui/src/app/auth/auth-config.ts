import { environment } from 'src/environments/environment';
import { AuthConfig } from '@auth0/auth0-angular';

export interface AuthenticationConfig {
  provider: 'dummy' | 'auth0';
  environment: string;
  apiBaseUrl: string;
  issuer?: string;
  audience?: string;
  clientId?: string;
}

export let authenticationConfig: AuthenticationConfig = {
  provider: environment.name === 'local' ? 'dummy' : 'auth0',
  environment: environment.name,
  apiBaseUrl: environment.apiBaseUrl,
};

export function validateAuthenticationConfig(config: AuthenticationConfig): void {
  if (
    !config ||
    config.environment !== environment.name ||
    config.apiBaseUrl !== environment.apiBaseUrl ||
    !['dummy', 'auth0'].includes(config.provider)
  ) {
    throw new Error('Authentication configuration does not match this deployment');
  }
  if (config.provider === 'dummy' && environment.name !== 'local') {
    throw new Error('Dummy authentication is local-only');
  }
  if (config.provider === 'auth0') {
    if (!config.issuer || !config.audience || !config.clientId)
      throw new Error('Missing Auth0 configuration');
    const issuer = new URL(config.issuer);
    if (issuer.protocol !== 'https:' || config.issuer !== issuer.origin + '/') {
      throw new Error('Auth0 issuer must be an HTTPS origin with trailing slash');
    }
  }
}

export async function loadAuthenticationConfig(): Promise<void> {
  if (environment.name === 'local') {
    if (!['localhost', '127.0.0.1', '[::1]'].includes(window.location.hostname)) {
      throw new Error('Local authentication builds cannot be deployed');
    }
    return;
  }
  const response = await fetch('/auth-config.json', {
    cache: 'no-store',
    credentials: 'same-origin',
  });
  if (!response.ok) throw new Error('Authentication configuration unavailable');
  const config: AuthenticationConfig = await response.json();
  validateAuthenticationConfig(config);
  authenticationConfig = config;
}

export function auth0SdkConfig(): AuthConfig {
  const config = authenticationConfig;
  if (config.provider !== 'auth0') throw new Error('Auth0 is not selected');
  validateAuthenticationConfig(config);
  return {
    domain: new URL(config.issuer!).host,
    clientId: config.clientId!,
    errorPath: '/auth-error',
    authorizationParams: {
      redirect_uri: window.location.origin + '/',
      audience: config.audience,
      scope: 'openid profile email offline_access',
    },
    cacheLocation: 'memory',
    useRefreshTokens: true,
    // A full reload loses the in-memory refresh token. Restore through Auth0 SSO when
    // browser cookie policy permits; otherwise show login. This is not provider fallback.
    useRefreshTokensFallback: true,
  };
}
