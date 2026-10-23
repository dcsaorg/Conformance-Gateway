import { ChangeDetectorRef, Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { NgForm } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { MessageDialog } from 'src/app/dialogs/message/message-dialog.component';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-auth-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: false,
})
export class LoginComponent implements OnInit {
  isLoading = true;
  get usesRedirectLogin(): boolean {
    return this.authService.usesRedirectLogin;
  }

  constructor(
    private authService: AuthService,
    private dialog: MatDialog,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  async ngOnInit() {
    if (await this.authService.isAuthenticated()) {
      this.router.navigate(['/']);
      return;
    }
    this.isLoading = false;
    this.cdr.detectChanges();
  }

  async onLocalLogin(form: NgForm) {
    if (form.invalid) {
      return;
    }

    const email = form.value.email;

    try {
      this.isLoading = true;
      await this.authService.login(email);
    } catch {
      this.isLoading = false;
      this.cdr.detectChanges();
      await MessageDialog.open(
        this.dialog,
        'Login failed',
        'Unable to start the local development session.',
      );
      this.cdr.detectChanges();
      form.reset();
      return;
    }

    this.isLoading = false;
    this.cdr.detectChanges();
    this.router.navigate(['/']);
  }

  async onUniversalLogin(): Promise<void> {
    this.isLoading = true;
    try {
      await this.authService.login();
    } catch {
      this.isLoading = false;
      this.cdr.detectChanges();
      await MessageDialog.open(
        this.dialog,
        'Login failed',
        'Unable to sign in. Please retry or contact DCSA support.',
      );
    }
  }
}
