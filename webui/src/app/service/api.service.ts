import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Injectable} from "@angular/core";
import {firstValueFrom} from "rxjs";
import {environment} from "src/environments/environment";
import {AuthService} from "../auth/auth.service";
import {handleApiCall} from "../helpers/api-error-handler";
import {Router} from '@angular/router';

@Injectable({
    providedIn: 'root'
})
export class ApiService {

    private readonly apiUrl: string = environment.apiBaseUrl + 'conformance/webui';

    constructor(
        private readonly authService: AuthService,
        private readonly httpClient: HttpClient,
        private readonly router: Router,
    ) {
    }

    async call(request: any,): Promise<any> {
        try {
            return handleApiCall(await this.send(request));
        } catch (e: any) {
            if (e.status === 403) await this.router.navigate(['/access-denied']);
            else if (e.status === 401 || !await this.authService.isAuthenticated()) {
                await this.authService.logout();
                await this.router.navigate(['/login']);
            }
            return handleApiCall(e.error ?? {error: 'Request failed. Please retry.'});
        }
    }

    /**
     * HTTP 200 with {} means backend admission and personal ownership resolution succeeded.
     * First access may create identity mappings. This is not a read-only health check.
     * Bypass call() so the route guard receives the original 401/403/409/503 status.
     */
    async checkAccess(): Promise<void> {
        const response = await this.send({operation: 'checkAccess'});
        if (!response || response.error) throw {status: 503};
    }

    private async send(request: any): Promise<any> {
        const authorization = await this.authService.getApiAuthorization();
        if (!authorization) throw {status: 401};
        const headers = new HttpHeaders({'Authorization': authorization});

        return firstValueFrom(
                this.httpClient.post<any>(
                    this.apiUrl,
                    request,
                    {
                        headers,
                    },
                )
            );
    }
}
