import { Component, OnInit } from "@angular/core";
import { NgForm } from "@angular/forms";
import { MatDialog } from "@angular/material/dialog";
import { Router } from "@angular/router";
import { MessageDialog } from "src/app/dialogs/message/message-dialog.component";
import { AuthService } from "../../auth/auth.service";

@Component({
  selector: 'app-auth-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  isLoading = true;

  constructor(
    private authService: AuthService,
    private dialog: MatDialog,
    private router: Router,
  ) {}

  async ngOnInit() {
    if (await this.authService.isAuthenticated()) {
      this.router.navigate(['/']);
      return;
    }
    this.isLoading = false;
  }

  async onLogin(form: NgForm) {
    if (form.invalid) {
      return;
    }

    const email = form.value.email;
    const password = form.value.password;

    try {
      this.isLoading = true;
      await this.authService.login(
        email,
        password
      );
    } catch (error) {
      this.isLoading = false;
      await MessageDialog.open(
        this.dialog,
        'Login failed',
        (error as any).error || error
      );
      form.reset();
      return;
    }
    
    this.isLoading = false;
    this.router.navigate(['/']);
  }
}
