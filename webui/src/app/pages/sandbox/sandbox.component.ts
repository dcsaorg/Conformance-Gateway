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

@Component({
  selector: 'app-sandbox',
  templateUrl: './sandbox.component.html',
  styleUrls: ['../../shared-styles.css']
})
export class SandboxComponent {
  sandboxId: string = '';
  sandbox: Sandbox | undefined;
  scenarios: ScenarioDigest[] = [];
  isAnyScenarioRunning: boolean = false;

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
        await this._loadScenarios();
      });
  }

  async _loadScenarios() {
    this.sandbox = await this.conformanceService.getSandbox(this.sandboxId);
    this.scenarios = await this.conformanceService.getScenarioDigests(this.sandboxId);
    this.isAnyScenarioRunning = this.scenarios.filter(scenario => scenario.isRunning).length > 0;
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
        await this._loadScenarios();
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
}
