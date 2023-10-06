import { Component } from "@angular/core";
import { ConformanceService } from "../../service/conformance.service";
import { Router } from "@angular/router";
import { AuthService } from "../../auth/auth.service";
import { Sandbox } from "../../model/sandbox";

@Component({
  selector: 'app-environment',
  templateUrl: './environment.component.html',
  styleUrls: ['../../shared-styles.css']
})
export class EnvironmentComponent {

  isLoading: boolean = true;
  sandboxes: Sandbox[] = [];

  constructor(
    public authService: AuthService,
    public conformanceService: ConformanceService,
    private router: Router,
  ) {}

  async ngOnInit() {
    this.isLoading = true;
    if (!await this.authService.isAuthenticated()) {
      this.router.navigate([
        '/login'
      ]);
      return;
    }
    this.sandboxes = await this.conformanceService.getAllSandboxes();
    this.isLoading = false;
  }

  onSandboxClick(sandbox: Sandbox) {
    this.router.navigate([
      '/sandbox', sandbox.id
    ]);
  }

  onCreateSandbox() {
    this.router.navigate([
      '/create-sandbox'
    ]);
  }
}
