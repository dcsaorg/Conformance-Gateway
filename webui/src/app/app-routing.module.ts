import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";

import { LoginComponent } from "./pages/login/login.component";
import { EnvironmentComponent } from "./pages/environment/environment.component";
import { SandboxComponent } from "./pages/sandbox/sandbox.component";
import { ScenarioComponent } from "./pages/scenario/scenario.component";
import { CreateSandboxComponent } from "./pages/create-sandbox/create-sandbox.component";
import { EditSandboxComponent } from "./pages/edit-sandbox/edit-sandbox.component";
import { accessGuard, signedInGuard } from './auth/access.guard';
import { AccessDeniedComponent, AuthenticationErrorComponent } from './pages/access-denied/access-denied.component';

const routes: Routes = [
  { path: '', redirectTo: 'environment', pathMatch: 'full' },

  { path: 'login', component: LoginComponent },
  { path: 'access-denied', component: AccessDeniedComponent, canActivate: [signedInGuard] },
  { path: 'auth-error', component: AuthenticationErrorComponent },

  { path: 'environment', component: EnvironmentComponent, canActivate: [accessGuard] },

  { path: 'sandbox/:sandboxId', component: SandboxComponent, canActivate: [accessGuard] },
  { path: 'create-sandbox', component: CreateSandboxComponent, canActivate: [accessGuard] },
  { path: 'edit-sandbox/:sandboxId', component: EditSandboxComponent, canActivate: [accessGuard] },

  { path: 'scenario/:sandboxId/:scenarioId', component: ScenarioComponent, canActivate: [accessGuard] },

  { path: '**', redirectTo: '/', pathMatch: 'full' },
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes),
  ],
  exports: [
    RouterModule
  ],
  providers: [],
})
export class AppRoutingModule {
}
