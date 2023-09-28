import { v4 as uuid4 } from "uuid";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Router } from "@angular/router";
import { firstValueFrom } from "rxjs";
import { environment } from "src/environments/environment";
import { AuthService } from "../auth/auth.service";

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private apiUrl: string = environment.apiBaseUrl + 'conformance/webui';

  constructor(
    private authService: AuthService,
    private httpClient: HttpClient,
    private router: Router,
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

    if (response.isError) {
      throw new Error(response);
    }
    return response;
  }
}
