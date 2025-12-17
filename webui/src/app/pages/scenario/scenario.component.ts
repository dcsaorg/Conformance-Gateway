import {ChangeDetectorRef, Component, OnDestroy, OnInit} from "@angular/core";
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
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly cdr: ChangeDetectorRef,
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
        this.cdr.detectChanges();
      });
  }

  async loadScenarioStatus() {
    this.actionInput = '';
    this.sandboxStatus = undefined;
    this.scenarioStatus = undefined;
    this.cdr.detectChanges(); // Immediately update UI to show loading state

    const sandboxStatusCheckStartTime = new Date().getTime();
    while (true) {
      this.sandboxStatus = await this.conformanceService.getSandboxStatus(this.sandbox!.id);
      if (this.sandboxStatus.waiting.length == 0
        || new Date().getTime() - sandboxStatusCheckStartTime >= 60 * 1000) {
        break;
      }
      console.log("loadScenarioStatus() sandbox waiting: " + JSON.stringify(this.sandboxStatus.waiting, null, 4));
      this.cdr.detectChanges(); // Update UI to show waiting status immediately
      await sleep(500); // Reduced from 1000ms to 500ms for more responsive polling
    }

    this.scenarioStatus = await this.conformanceService.getScenarioStatus(
      this.sandbox!.id,
      this.scenario!.id
    );
    this.actionInput = JSON.stringify(this.scenarioStatus?.jsonForPromptText, null, 4);
    this.cdr.detectChanges();
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
      this.cdr.detectChanges(); // Immediately show the "performing action" message

      const response: any = await this.conformanceService.completeCurrentAction(this.sandbox!.id, false);
      if (response?.error) {
        this.performingAction = "";
        this.cdr.detectChanges();
        await MessageDialog.open(
            this.dialog,
            "Error completing action",
            response.error)
        return
      }
      this.performingAction = "";
      await this.loadScenarioStatus();
    }
  }

  async skipCurrentAction() {
    this.performingAction = "Marking current action as skipped...";
    this.cdr.detectChanges(); // Immediately show the "performing action" message

    const response: any = await this.conformanceService.completeCurrentAction(this.sandbox!.id, true);
    if (response?.error) {
      this.performingAction = "";
      this.cdr.detectChanges();
      await MessageDialog.open(
          this.dialog,
          "Error skipping action",
          response.error)
      return
    }
    this.performingAction = "";
    await this.loadScenarioStatus();
  }

  async viewHttpExchanges() {
    const response = await this.conformanceService.getCurrentActionExchanges(
      this.sandbox!.id,
      this.scenario!.id
    );

    if (response?.error) {
      await MessageDialog.open(
          this.dialog,
          "Error retrieving http exchanges",
          response.error);
      return
    }
    await TextDialog.open(
        this.dialog,
        "Current action HTTP exchanges",
        JSON.stringify(response, null, 4)
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
