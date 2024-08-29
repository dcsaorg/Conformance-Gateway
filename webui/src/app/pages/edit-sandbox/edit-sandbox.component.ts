import { Component } from "@angular/core";
import { ConformanceService } from "../../service/conformance.service";
import { ActivatedRoute, Router } from "@angular/router";
import { AuthService } from "../../auth/auth.service";
import { SandboxConfig } from "src/app/model/sandbox-config";
import { Subscription } from "rxjs";

@Component({
  selector: 'app-edit-sandbox',
  templateUrl: './edit-sandbox.component.html',
  styleUrls: ['../../shared-styles.css']
})
export class EditSandboxComponent {
  sandboxId: string = '';
  originalSandboxConfig: SandboxConfig | undefined;
  updatedSandboxConfig: SandboxConfig | undefined;
  updatingSandbox: boolean = false;

  activatedRouteSubscription: Subscription | undefined;

  constructor(
    public activatedRoute: ActivatedRoute,
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
    this.activatedRouteSubscription = this.activatedRoute.params.subscribe(
      async params => {
        this.sandboxId = params['sandboxId'];
        this.originalSandboxConfig = await this.conformanceService.getSandboxConfig(this.sandboxId);
        this.updatedSandboxConfig = JSON.parse(JSON.stringify(this.originalSandboxConfig));
      });
  }

  async ngOnDestroy() {
    if (this.activatedRouteSubscription) {
      this.activatedRouteSubscription.unsubscribe();
    }
  }

  onAddHeader() {
    this.updatedSandboxConfig?.externalPartyAdditionalHeaders.push({headerName: '', headerValue: ''});
  }

  onRemoveHeader() {
    this.updatedSandboxConfig?.externalPartyAdditionalHeaders.pop();
  }

  headerNameRegex = /^[!#$%&'*+.^_`|~\w-]+$/;
  headerValueRegex = /^[\t\x20-\x7E\x80-\xFF]*$/;

  cannotUpdate(): boolean {
    if (!this.originalSandboxConfig || !this.updatedSandboxConfig) return true;
    for (let additionalHeader of this.updatedSandboxConfig.externalPartyAdditionalHeaders) {
      if (!this.headerNameRegex.test(additionalHeader.headerName)) return true;
      if (!this.headerValueRegex.test(additionalHeader.headerValue)) return true;
    }
    return JSON.stringify(this.originalSandboxConfig) === JSON.stringify(this.updatedSandboxConfig);
  }

  async onUpdate() {
    if (this.cannotUpdate()) return;
    this.updatingSandbox = true;
    await this.conformanceService.updateSandboxConfig(
      this.sandboxId,
      this.updatedSandboxConfig!.sandboxName,
      this.updatedSandboxConfig!.externalPartyUrl,
      this.updatedSandboxConfig!.externalPartyAuthHeaderName,
      this.updatedSandboxConfig!.externalPartyAuthHeaderValue,
      this.updatedSandboxConfig!.externalPartyAdditionalHeaders,
    );
    this.router.navigate([
      "/sandbox", this.sandboxId
    ]);
  }

  onCancel() {
    this.router.navigate([
      "/sandbox", this.sandboxId
    ]);
  }
}
