import { Component } from "@angular/core";
import { ConformanceService } from "../../service/conformance.service";
import { ActivatedRoute, Router } from "@angular/router";
import { AuthService } from "../../auth/auth.service";
import { Sandbox } from "../../model/sandbox";
import { Subscription } from "rxjs";
import { ScenarioDigest } from "src/app/model/scenario";
import { ConformanceStatus,
  getConformanceStatusEmoji,
  getConformanceStatusTitle
} from "src/app/model/conformance-status";
import { ScenarioStatus } from "src/app/model/scenario-status";
import {ConfirmationDialog} from "../../dialogs/confirmation/confirmation-dialog.component";
import {MatDialog} from "@angular/material/dialog";
import {SandboxStatus} from "../../model/sandbox-status";

@Component({
  selector: 'app-scenario',
  templateUrl: './scenario.component.html',
  styleUrls: ['../../shared-styles.css']
})
export class ScenarioComponent {

  sandboxStatus: SandboxStatus | undefined;
  sandbox: Sandbox | undefined;
  scenario: ScenarioDigest | undefined;
  scenarioStatus: ScenarioStatus | undefined;
  actionInput: string = '';
  processingActionInput: boolean = false;
  activatedRouteSubscription: Subscription | undefined;

  getConformanceStatusEmoji = getConformanceStatusEmoji;
  getConformanceStatusTitle = getConformanceStatusTitle;

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
        const sandboxId: string = params['sandboxId'];
        const scenarioId: string = params['scenarioId'];
        this.sandbox = await this.conformanceService.getSandbox(sandboxId, false);
        this.scenario = await this.conformanceService.getScenario(sandboxId, scenarioId);
        await this.loadScenarioStatus();
      });
  }

  async loadScenarioStatus() {
    this.actionInput = '';
    this.sandboxStatus = undefined;
    this.scenarioStatus = undefined;
    this.sandboxStatus = await this.conformanceService.getSandboxStatus(this.sandbox!.id);
    console.log("sandboxStatus=" + JSON.stringify(this.sandboxStatus));
    this.scenarioStatus = await this.conformanceService.getScenarioStatus(
      this.sandbox!.id,
      this.scenario!.id
    );
  }

  async completeCurrentAction() {
    if (await ConfirmationDialog.open(
      this.dialog,
      "Complete action",
      "Are you sure you want to complete the current action? "
      + "You cannot go back to a previous action without restarting the scenario.")
    ) {
      await this.conformanceService.completeCurrentAction(this.sandbox!.id);
      await this.loadScenarioStatus();
    }
  }

  async ngOnDestroy() {
    if (this.activatedRouteSubscription) {
      this.activatedRouteSubscription.unsubscribe();
    }
  }

  getJsonForPromptText(): string {
    return JSON.stringify(this.scenarioStatus?.jsonForPromptText, null, 4);
  }

  cannotSubmit(): boolean {
    return this.actionInput.trim() === '';
  }

  async onSubmit(withInput: boolean) {
    this.processingActionInput = true;
    await this.conformanceService.handleActionInput(
      this.sandbox!.id,
      this.scenario!.id,
      this.scenarioStatus!.promptActionId,
      withInput ? (this.scenarioStatus?.jsonForPromptText ? JSON.parse(this.actionInput.trim()) : this.actionInput.trim()) : undefined);
    this.processingActionInput = false;
    await this.loadScenarioStatus();
  }
}
