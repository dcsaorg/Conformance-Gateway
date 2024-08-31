import { Component } from "@angular/core";
import { ConformanceService } from "../../service/conformance.service";
import { ActivatedRoute, Router } from "@angular/router";
import { AuthService } from "../../auth/auth.service";
import { Sandbox } from "../../model/sandbox";
import { sleep } from "../../model/toolkit";
import { Subscription } from "rxjs";
import { ScenarioDigest } from "src/app/model/scenario";
import { ConformanceStatus,
  getConformanceStatusEmoji,
  getConformanceStatusTitle
} from "src/app/model/conformance-status";
import { ScenarioStatus } from "src/app/model/scenario-status";
import {ConfirmationDialog} from "../../dialogs/confirmation/confirmation-dialog.component";
import {MatDialog} from "@angular/material/dialog";
import {SandboxStatus, SandboxWaiting} from "../../model/sandbox-status";
import {MessageDialog} from "../../dialogs/message/message-dialog.component";

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
  performingAction: string = '';
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

    const sandboxStatusCheckStartTime = new Date().getTime();
    do {
      this.sandboxStatus = await this.conformanceService.getSandboxStatus(this.sandbox!.id);
      console.log("sandboxStatus=" + JSON.stringify(this.sandboxStatus));
      await sleep(1000);
    } while (this.sandboxStatus.waiting.length > 0
      && new Date().getTime() - sandboxStatusCheckStartTime < 60 * 1000);

    this.scenarioStatus = await this.conformanceService.getScenarioStatus(
      this.sandbox!.id,
      this.scenario!.id
    );
  }

  formattedSandboxWaiting(sandboxWaiting: SandboxWaiting): string {
    return `${sandboxWaiting.who} is waiting for ${sandboxWaiting.forWhom} to ${sandboxWaiting.toDoWhat}`;
  }

  async completeCurrentAction() {
    if (await ConfirmationDialog.open(
      this.dialog,
      "Action completed",
      "Are you sure you want to mark the current action as completed? "
      + "You cannot go back to a previous action without restarting the scenario.")
    ) {
      this.performingAction = "Marking current action as completed...";
      const response: any = await this.conformanceService.completeCurrentAction(this.sandbox!.id);
      if (response.error) {
        await MessageDialog.open(
          this.dialog,
          "Error completing action",
          response.error)
      }
      this.performingAction = "";
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

  getCurrentActionTitle(): string {
    return this.scenarioStatus?.nextActions.split(" - ")[0] || "";
  }

  cannotSubmit(): boolean {
    return this.actionInput.trim() === '';
  }

  async onSubmit(withInput: boolean) {
    this.performingAction = "Processing action input...";
    const response:any = await this.conformanceService.handleActionInput(
      this.sandbox!.id,
      this.scenario!.id,
      this.scenarioStatus!.promptActionId,
      withInput ? (this.scenarioStatus?.jsonForPromptText ? JSON.parse(this.actionInput.trim()) : this.actionInput.trim()) : undefined);
    if (response.error) {
      await MessageDialog.open(
        this.dialog,
        "Error processing input",
        response.error)
    }
    this.performingAction = "";
    await this.loadScenarioStatus();
  }
}
