import { Component } from "@angular/core";
import { ConformanceService } from "../../service/conformance.service";
import { Router } from "@angular/router";
import { AuthService } from "../../auth/auth.service";
import { Standard, StandardVersion } from "src/app/model/standard";

@Component({
  selector: 'app-create-sandbox',
  templateUrl: './create-sandbox.component.html',
  styleUrls: ['../../shared-styles.css']
})
export class CreateSandboxComponent {
  SANDBOX_TYPES: string[] = [
    "Test orchestrator and counterparts (default)",
    "Tested party (DCSA internal use only)"
  ];

  standards: Standard[] = [];
  selectedStandard: Standard | undefined;
  selectedVersion: StandardVersion | undefined;
  selectedRole: string | undefined;
  selectedSandboxType: string | undefined;
  newSandboxName: string = '';

  constructor(
    public authService: AuthService,
    public conformanceService: ConformanceService,
    private router: Router,
  ) {}

  async ngOnInit() {
    if (!await this.authService.isAuthenticated()) {
      this.router.navigate([
        '/login'
      ]);
      return;
    }
    this.standards = await this.conformanceService.getAvailableStandards();
  }

  onSelectedStandardChanged(standard: Standard | undefined) {
    this.selectedVersion = undefined;
  }

  onSelectedVersionChanged(version: StandardVersion | undefined) {
    this.selectedRole = undefined;
  }

  onSelectedRoleChanged(role: string | undefined) {
    this.selectedSandboxType = undefined;
  }

  onSelectedSandboxTypeChanged(sandboxType: string | undefined) {
    // meh
  }

  cannotCreate(): boolean {
    return !this.selectedStandard || !this.selectedVersion || !this.selectedRole || !this.selectedSandboxType || !this.newSandboxName;
  }

  async onCreate() {
    if (this.cannotCreate()) return;
    const sandboxId: string = await this.conformanceService.createSandbox(
      this.selectedStandard!.name,
      this.selectedVersion!.number,
      this.selectedRole!,
      this.selectedSandboxType === this.SANDBOX_TYPES[0],
      this.newSandboxName
    );
    this.router.navigate([
      "/edit-sandbox", sandboxId
    ]);
  }

  onCancel() {
    this.router.navigate(["/"]);
  }
}
