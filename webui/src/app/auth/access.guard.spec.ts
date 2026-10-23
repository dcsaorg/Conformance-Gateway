import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { accessGuard, signedInGuard } from './access.guard';
import { AuthService } from './auth.service';
import { ApiService } from '../service/api.service';

describe('Conformance admission', () => {
  let auth: any, api: any, router: any;
  beforeEach(() => {
    auth = {
      isAuthenticated: jest.fn(async () => true),
      logout: jest.fn(async () => {}),
      getApiAuthorization: jest.fn(async () => 'Bearer access-token'),
    };
    api = { checkAccess: jest.fn(async () => {}) };
    router = { createUrlTree: jest.fn(([path]) => path), navigate: jest.fn(async () => true) };
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: ApiService, useValue: api },
        { provide: Router, useValue: router },
      ],
    });
  });
  afterEach(() => sessionStorage.clear());
  const guard = () =>
    TestBed.runInInjectionContext(() => accessGuard({} as any, { url: '/sandbox/private' } as any));

  it('direct unauthenticated links go to login and retain their safe return path', async () => {
    auth.isAuthenticated.mockResolvedValue(false);
    expect(await guard()).toBe('/login');
    expect(api.checkAccess).not.toHaveBeenCalled();
    expect(sessionStorage.getItem('conformance.returnTo')).toBe('/sandbox/private');
  });
  it('allows access only after a successful backend probe', async () => {
    expect(await guard()).toBe(true);
    expect(api.checkAccess).toHaveBeenCalledTimes(1);
  });
  it('keeps authenticated users without permission on access-denied, not login', async () => {
    api.checkAccess.mockRejectedValue({ status: 403 });
    expect(await guard()).toBe('/access-denied');
    expect(auth.logout).not.toHaveBeenCalled();
    expect(await TestBed.runInInjectionContext(() => signedInGuard({} as any, {} as any))).toBe(
      true,
    );
  });
  it('distinguishes expired credentials from migration/provider failures', async () => {
    api.checkAccess.mockRejectedValue({ status: 401 });
    expect(await guard()).toBe('/login');
    api.checkAccess.mockRejectedValue({ status: 409 });
    expect(await guard()).toBe('/auth-error');
    api.checkAccess.mockRejectedValue({ status: 503 });
    expect(await guard()).toBe('/auth-error');
  });
  it('attaches only the provider credential to the configured API and preserves request data', async () => {
    const http = { post: jest.fn(() => of({ sandboxes: [] })) };
    const service = new ApiService(auth, http as any, router);
    expect(await service.call({ operation: 'getAllSandboxes' })).toEqual({ sandboxes: [] });
    const [url, body, options] = http.post.mock.calls[0] as any;
    expect(url).toBe('http://localhost:8080/conformance/webui');
    expect(body).toEqual({ operation: 'getAllSandboxes' });
    expect(options.headers.get('Authorization')).toBe('Bearer access-token');
  });
  it('never retries forbidden API operations and clears local auth after a 401', async () => {
    const http = {
      post: jest.fn(() => throwError(() => ({ status: 403, error: { error: 'forbidden' } }))),
    };
    const service = new ApiService(auth, http as any, router);
    await service.call({ operation: 'createSandbox' });
    expect(router.navigate).toHaveBeenCalledWith(['/access-denied']);
    expect(auth.logout).not.toHaveBeenCalled();
    expect(http.post).toHaveBeenCalledTimes(1);
    http.post.mockImplementation(() =>
      throwError(() => ({ status: 401, error: { error: 'unauthorized' } })),
    );
    await service.call({ operation: 'getAllSandboxes' });
    expect(auth.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
  it('fails closed on a legacy HTTP-200 application error from the probe', async () => {
    const service = new ApiService(
      auth,
      { post: () => of({ error: 'not configured' }) } as any,
      router,
    );
    await expect(service.checkAccess()).rejects.toEqual({ status: 503 });
  });
  it('uses the empty admission response without performing a business operation', async () => {
    const http = { post: jest.fn(() => of({})) };
    await expect(new ApiService(auth, http as any, router).checkAccess()).resolves.toBeUndefined();
    const [, body, options] = http.post.mock.calls[0] as any;
    expect(body).toEqual({ operation: 'checkAccess' });
    expect(options.headers.get('Authorization')).toBe('Bearer access-token');
    expect(router.navigate).not.toHaveBeenCalled();
  });
  it.each([401, 403, 409, 503])('preserves probe status %s for the admission guard', async status => {
    const http = { post: jest.fn(() => throwError(() => ({ status }))) };
    await expect(new ApiService(auth, http as any, router).checkAccess()).rejects.toEqual({ status });
    expect(http.post).toHaveBeenCalledTimes(1);
    expect(auth.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
