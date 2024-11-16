import { Component } from "@angular/core";
import { ConformanceService } from "../../service/conformance.service";
import { ActivatedRoute, Router } from "@angular/router";
import { AuthService } from "../../auth/auth.service";
import { SandboxConfig } from "src/app/model/sandbox-config";
import { Subscription } from "rxjs";
import { MessageDialog } from "../../dialogs/message/message-dialog.component";
import { MatDialog } from "@angular/material/dialog";

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
    private dialog: MatDialog,
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

  onUsingCustomEndpointUrisChange(enabled: boolean): void {
    if (enabled) {
      this.updatedSandboxConfig!.externalPartyEndpointUriOverrides = [];
      for (let endpointUriMethod of this.updatedSandboxConfig!.externalPartyEndpointUriMethods) {
        for (let method of endpointUriMethod.methods) {
          const suffixStart = endpointUriMethod.endpointUri.indexOf("/{")
          this.updatedSandboxConfig!.externalPartyEndpointUriOverrides.push(
            suffixStart > 0 ? {
              method,
              endpointBaseUri: endpointUriMethod.endpointUri.substring(0, suffixStart),
              endpointSuffix: endpointUriMethod.endpointUri.substring(suffixStart),
              baseUriOverride: endpointUriMethod.endpointUri.substring(0, suffixStart),
            } : {
              method,
              endpointBaseUri: endpointUriMethod.endpointUri,
              endpointSuffix: "",
              baseUriOverride: endpointUriMethod.endpointUri,
            }
          );
        }
      }
    } else {
      this.updatedSandboxConfig!.externalPartyEndpointUriOverrides = undefined;
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

    if (this.updatedSandboxConfig.externalPartyAuthHeaderName) {
      if (!this.headerNameRegex.test(this.updatedSandboxConfig.externalPartyAuthHeaderName)) return true;
      if (!this.headerValueRegex.test(this.updatedSandboxConfig.externalPartyAuthHeaderValue)) return true;
    }
    for (let additionalHeader of this.updatedSandboxConfig.externalPartyAdditionalHeaders) {
      if (!this.headerNameRegex.test(additionalHeader.headerName)) return true;
      if (!this.headerValueRegex.test(additionalHeader.headerValue)) return true;
    }
    return JSON.stringify(this.originalSandboxConfig) === JSON.stringify(this.updatedSandboxConfig);
  }

  async onUpdate() {
    if (this.cannotUpdate()) return;
    this.updatingSandbox = true;
    const response: any = await this.conformanceService.updateSandboxConfig(
      this.sandboxId,
      this.updatedSandboxConfig!.sandboxName,
      this.updatedSandboxConfig!.externalPartyUrl,
      this.updatedSandboxConfig!.externalPartyAuthHeaderName,
      this.updatedSandboxConfig!.externalPartyAuthHeaderValue,
      this.updatedSandboxConfig!.externalPartyAdditionalHeaders,
      this.updatedSandboxConfig!.externalPartyEndpointUriOverrides,
    );
    if (response?.error) {
      await MessageDialog.open(
        this.dialog,
        "Error completing action",
        response.error);
      this.updatingSandbox = false;
    } else {
      this.router.navigate([
        "/sandbox", this.sandboxId
      ]);
    }
  }

  onCancel() {
    this.router.navigate([
      "/sandbox", this.sandboxId
    ]);
  }
}
