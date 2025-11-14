import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Injectable} from "@angular/core";
import {firstValueFrom} from "rxjs";
import {environment} from "src/environments/environment";
import {AuthService} from "../auth/auth.service";
import {handleApiCall} from "../helpers/api-error-handler";

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private readonly apiUrl: string = environment.apiBaseUrl + 'conformance/webui';

  constructor(
    private readonly authService: AuthService,
    private readonly httpClient: HttpClient,
  ) {
  }

    async call(
        request: any,
    ): Promise<any> {
        const userIdToken: string | null = await this.authService.getUserIdToken();
        const headers: HttpHeaders | undefined = (
            userIdToken
                ? new HttpHeaders({
                    'Authorization': userIdToken,
                })
                : undefined
        );

        const response: any = await firstValueFrom(
            this.httpClient.post<any>(
                this.apiUrl,
                request,
                {
                    headers,
                },
            )
        );

       return handleApiCall(response);
    }
}
