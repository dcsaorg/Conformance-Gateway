import { Component } from "@angular/core";
import { ConformanceService } from "../../service/conformance.service";
import { ActivatedRoute, Router } from "@angular/router";
import { AuthService } from "../../auth/auth.service";
import { Sandbox } from "../../model/sandbox";
import { Subscription } from "rxjs";
import { ScenarioDigest } from "src/app/model/scenario";
import {
  ConformanceStatus,
  getConformanceStatusEmoji,
  getConformanceStatusTitle
} from "src/app/model/conformance-status";
import { ConfirmationDialog } from "src/app/dialogs/confirmation/confirmation-dialog.component";
import { MatDialog } from "@angular/material/dialog";
import {StandardModule} from "../../model/standard-module";

@Component({
  selector: 'app-sandbox',
  templateUrl: './sandbox.component.html',
  styleUrls: ['../../shared-styles.css']
})
export class SandboxComponent {
  sandboxId: string = '';
  sandbox: Sandbox | undefined;
  standardModules: StandardModule[] = [];
  isAnyScenarioRunning: boolean = false;
  isLoading: boolean = false;

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
        this.sandboxId = params['sandboxId'];
        await this._loadData();
      });
  }

  async _loadData() {
    this.isLoading = true;
    this.sandbox = await this.conformanceService.getSandbox(this.sandboxId, true);
    this.standardModules = await this.conformanceService.getScenarioDigests(this.sandboxId);
    this.isAnyScenarioRunning = this.standardModules.filter(
      module => module.scenarios.filter(
        scenario => scenario.isRunning
      ).length > 0
    ).length > 0;
    this.isLoading = false;
  }

  async ngOnDestroy() {
    if (this.activatedRouteSubscription) {
      this.activatedRouteSubscription.unsubscribe();
    }
  }

  getActionIconName(scenario: ScenarioDigest): string {
    return scenario.isRunning
        ? "stop"
        : scenario.conformanceStatus === ConformanceStatus.NO_TRAFFIC
            ? "play_arrow"
            : "replay";
  }

  getActionTitle(scenario: ScenarioDigest): string {
    return scenario.isRunning
        ? "Stop"
        : scenario.conformanceStatus === ConformanceStatus.NO_TRAFFIC
            ? "Start"
            : "Restart";
  }

  cannotPerformAction(scenario: ScenarioDigest): boolean {
    return this.isAnyScenarioRunning && !scenario.isRunning;
  }

  async onScenarioAction(event: MouseEvent, scenario: ScenarioDigest) {
    const action: string = this.getActionTitle(scenario);
    event.stopPropagation();
    if (this.cannotPerformAction(scenario)) return;
    if (
      (
        !scenario.isRunning
        &&
        scenario.conformanceStatus === ConformanceStatus.NO_TRAFFIC
      ) || await ConfirmationDialog.open(
          this.dialog,
          action + " scenario",
          "Are you sure you want to " + action.toLowerCase() + " the scenario? "
          + "All current scenario status and traffic will be lost.")
    ) {
      await this.conformanceService.startOrStopScenario(this.sandbox!.id, scenario.id);
      if (action === "Stop") {
        await this._loadData();
      } else {
        this.router.navigate([
          '/scenario', this.sandbox!.id, scenario.id
        ]);
      }
    }
  }

  async onScenarioClick(scenario: ScenarioDigest) {
    this.router.navigate([
      '/scenario', this.sandbox!.id, scenario.id
    ]);
  }

  onClickSettings() {
    this.router.navigate([
      '/edit-sandbox', this.sandbox!.id
    ]);
  }

  onClickNotifyParty() {
    this.conformanceService.notifyParty(this.sandbox!.id);
  }

  async onClickResetParty() {
    if (
      await ConfirmationDialog.open(
        this.dialog,
        "Reset party",
        "Are you sure you want to reset the party? "
        + "All current party data will be lost.")
    ) {
      this.conformanceService.resetParty(this.sandbox!.id);
    }
  }

  onClickRefresh() {
    this.sandbox = undefined;
    this._loadData();
  }
}
