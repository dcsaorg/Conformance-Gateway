import { Component, OnDestroy, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { Subscription } from "rxjs";
import { environment } from "src/environments/environment";
import { AuthService } from "../auth/auth.service";
// import { AuthService } from "../auth/auth.service";

@Component({
    selector: 'app-header',
    templateUrl: './header.component.html',
    styleUrls: ['./header.component.css'],
    standalone: false
})
export class HeaderComponent implements OnInit, OnDestroy {

  public readonly siteTitle: string = environment.siteTitle;

  public isAuthenticated: boolean = false;
  private authStatusSubscription: Subscription | null = null;

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {
  }

  async ngOnInit(): Promise<void> {
    this.isAuthenticated = await this.authService.initializeAuthentication();

    this.authStatusSubscription = this.authService.getAuthStatusObservable().subscribe(
      async (authenticated) => {
        if (authenticated !== this.isAuthenticated) {
          this.isAuthenticated = authenticated;
          if (!this.isAuthenticated) {
            this.router.navigate(['/login']);
          }
        }
      }
    );
  }

  ngOnDestroy(): void {
    this.authStatusSubscription?.unsubscribe();
  }

  async onLogout() {
    await this.authService.logout();
    this.router.navigate(['/login']);
  }

  async onHelp() {
    this.router.navigate(['/help']);
  }
}
