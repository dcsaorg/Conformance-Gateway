import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";

import { HomeComponent } from "./pages/home/home.component";
import { LoginComponent } from "./pages/login/login.component";
import { EnvironmentComponent } from "./pages/environment/environment.component";
import { SandboxComponent } from "./pages/sandbox/sandbox.component";
import { ScenarioComponent } from "./pages/scenario/scenario.component";
import { CreateSandboxComponent } from "./pages/create-sandbox/create-sandbox.component";
import { EditSandboxComponent } from "./pages/edit-sandbox/edit-sandbox.component";

const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: '', component: HomeComponent },

  { path: 'login', component: LoginComponent },

  { path: 'environment', component: EnvironmentComponent },

  { path: 'sandbox/:sandboxId', component: SandboxComponent },
  { path: 'create-sandbox', component: CreateSandboxComponent },
  { path: 'edit-sandbox/:sandboxId', component: EditSandboxComponent },

  { path: 'scenario/:sandboxId/:scenarioId', component: ScenarioComponent },

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
