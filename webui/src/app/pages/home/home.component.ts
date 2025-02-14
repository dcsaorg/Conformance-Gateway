import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { AuthService } from "../../auth/auth.service";

@Component({
    selector: 'app-home',
    templateUrl: './home.component.html',
    styleUrls: ['./home.component.css'],
    standalone: false
})
export class HomeComponent implements OnInit {

  isLoading = true;

  constructor(
    public authService: AuthService,
    private router: Router,
  ) {}

  async ngOnInit() {
    this.isLoading = true;
    if (await this.authService.isAuthenticated()) {
      this.router.navigate([
        '/environment'
      ]);
    } else {
      this.router.navigate([
        '/login'
      ]);
    }
    this.isLoading = false;
  }
}
