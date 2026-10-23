import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  standalone: true,
  template: `<h1>You do not have access to the Conformance Tool.</h1>
    <p>You are signed in, but your account does not have permission to use this application.
    Please contact DCSA support to request access.</p>`,
})
export class AccessDeniedComponent {}

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `<h1>We could not open your Conformance workspace.</h1>
    <p>Please retry. If the problem persists, contact DCSA support for help with your account.
    Your existing sandboxes and reports have not been changed.</p>
    <a routerLink="/environment">Retry</a> · <a routerLink="/login">Sign in</a>`,
})
export class AuthenticationErrorComponent {}
