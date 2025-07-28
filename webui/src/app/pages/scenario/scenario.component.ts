import {Component, OnDestroy, OnInit} from "@angular/core";
import { ConformanceService } from "../../service/conformance.service";
import { ActivatedRoute, Router } from "@angular/router";
import { AuthService } from "../../auth/auth.service";
import { Sandbox } from "../../model/sandbox";
import { sleep } from "../../model/toolkit";
import { Subscription } from "rxjs";
import { ScenarioDigest } from "src/app/model/scenario";
import { ScenarioStatus } from "src/app/model/scenario-status";
import {ConfirmationDialog} from "../../dialogs/confirmation/confirmation-dialog.component";
import {MatDialog} from "@angular/material/dialog";
import {SandboxStatus, SandboxWaiting} from "../../model/sandbox-status";
import {MessageDialog} from "../../dialogs/message/message-dialog.component";
import {TextDialog} from "../../dialogs/text/text-dialog.component";

@Component({
    selector: 'app-scenario',
    templateUrl: './scenario.component.html',
    styleUrls: ['../../shared-styles.css'],
    standalone: false
})
export class ScenarioComponent implements OnInit, OnDestroy {

  sandboxStatus: SandboxStatus | undefined;
  sandbox: Sandbox | undefined;
  scenario: ScenarioDigest | undefined;
  scenarioStatus: ScenarioStatus | undefined;
  actionInput: string = '';
  performingAction: string = '';
  activatedRouteSubscription: Subscription | undefined;

   submitAttempted: boolean = false;
    inlineErrorMessage: string = '';

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
    while (true) {
      this.sandboxStatus = await this.conformanceService.getSandboxStatus(this.sandbox!.id);
      if (this.sandboxStatus.waiting.length == 0
        || new Date().getTime() - sandboxStatusCheckStartTime >= 60 * 1000) {
        break;
      }
      console.log("loadScenarioStatus() sandbox waiting: " + JSON.stringify(this.sandboxStatus.waiting, null, 4));
      await sleep(1000);
    }

    this.scenarioStatus = await this.conformanceService.getScenarioStatus(
      this.sandbox!.id,
      this.scenario!.id
    );
    this.actionInput = JSON.stringify(this.scenarioStatus?.jsonForPromptText, null, 4);
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
      if (response?.error) {
        await MessageDialog.open(
          this.dialog,
          "Error completing action",
          response.error)
      }
      this.performingAction = "";
      await this.loadScenarioStatus();
    }
  }

  async skipCurrentAction() {
        this.performingAction = "Marking current action as skipped...";
        const response: any = await this.conformanceService.completeCurrentAction(this.sandbox!.id);
        if (response?.error) {
          await MessageDialog.open(
            this.dialog,
            "Error skipping action",
            response.error)
        }
        this.performingAction = "";
        await this.loadScenarioStatus();
    }

  async viewHttpExchanges() {
    const exchanges = await this.conformanceService.getCurrentActionExchanges(
      this.sandbox!.id,
      this.scenario!.id
    );
    await TextDialog.open(
      this.dialog,
      "Current action HTTP exchanges",
      JSON.stringify(exchanges, null, 4)
    );
  }

  async ngOnDestroy() {
    if (this.activatedRouteSubscription) {
      this.activatedRouteSubscription.unsubscribe();
    }
  }

  getCurrentActionTitle(): string {
    return this.scenarioStatus?.nextActions.split(" - ")[0] || "";
  }

  cannotSubmit(): boolean {
    return this.actionInput.trim() === '';
  }

  async onSubmit(withInput: boolean) {
    this.performingAction = "Processing action input...";
    this.submitAttempted = true;
    this.inlineErrorMessage = '';

    let serviceActionInput;

    try {
      serviceActionInput = withInput
        ? this.scenarioStatus?.jsonForPromptText
          ? JSON.parse(this.actionInput.trim())
          : this.actionInput.trim()
        : undefined;
    } catch (e) {
      this.inlineErrorMessage = e instanceof Error ? e.message : "Unknown error parsing JSON";
      this.performingAction = "";
      return;
    }

    const response: any = await this.conformanceService.handleActionInput(
      this.sandbox!.id,
      this.scenario!.id,
      this.scenarioStatus!.promptActionId,
      serviceActionInput
    );

    if (response?.error) {
      this.inlineErrorMessage = response.error;
      this.performingAction = "";
      return;
    }

    this.performingAction = "";
    await this.loadScenarioStatus();
  }
}
