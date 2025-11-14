import {Component, OnDestroy, OnInit} from "@angular/core";
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
import {ReportDigest} from "../../model/report-digest";
import {MessageDialog} from "../../dialogs/message/message-dialog.component";

@Component({
    selector: 'app-sandbox',
    templateUrl: './sandbox.component.html',
    styleUrls: ['../../shared-styles.css'],
    standalone: false
})
export class SandboxComponent implements OnInit, OnDestroy {
  sandboxId: string = '';
  sandbox: Sandbox | undefined;
  standardModules: StandardModule[] = [];
  reportDigests: ReportDigest[] = [];
  isAnyScenarioRunning: boolean = false;
  isLoading: boolean = false;
  startingOrStoppingScenario: boolean = false;
  newReportTitle: string = '';

  displayedReportDigest: ReportDigest | null = null;
  displayedReportContent: any | null = null;

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
    this.sandbox = undefined;
    this.standardModules = [];
    this.sandbox = await this.conformanceService.getSandbox(this.sandboxId, true);
    this.standardModules = await this.conformanceService.getScenarioDigests(this.sandboxId);
    this.isAnyScenarioRunning = this.standardModules.filter(
      module => module.scenarios.filter(
        scenario => scenario.isRunning
      ).length > 0
    ).length > 0;
    this.reportDigests = await this.conformanceService.getReportDigests(this.sandboxId);
    this.isLoading = false;
  }

  async ngOnDestroy() {
    if (this.activatedRouteSubscription) {
      this.activatedRouteSubscription.unsubscribe();
    }
  }

  isInternalSandbox(): boolean {
    return !!this.sandbox?.canNotifyParty;
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
          "Are you sure you want to " + action.toLowerCase() + " the scenario? " + (
              action === "Stop"
                ? "You cannot resume the scenario execution later; you can only restart the scenario,"
                + " at which point all current scenario status and traffic will be lost."
                : "All current scenario status and traffic will be lost."))
    ) {
      this.startingOrStoppingScenario = true;
      const response: any = await this.conformanceService.startOrStopScenario(this.sandbox!.id, scenario.id);
      if (response?.error) {
        await MessageDialog.open(
            this.dialog,
            "Error starting/stopping scenario",
            response.error);
        return
      }
      this.startingOrStoppingScenario = false;
      if (action === "Stop") {
        await this._loadData();
      } else {
        await this.router.navigate([
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

  async onClickDeleteSandbox() {
    if (
        await ConfirmationDialog.open(
            this.dialog,
            "Delete sandbox",
            "Are you sure you want to delete this sandbox? You cannot undo this operation.")
    ) {
      const response: any = await this.conformanceService.deleteSandbox(this.sandbox!.id);
      if (response?.error) {
        await MessageDialog.open(
            this.dialog,
            "Error deleting sandbox",
            response.error);
        return
      }
      await this.router.navigate([
        '/'
      ]);
    }
  }

  async onClickNotifyParty() {
    const response: any = await this.conformanceService.notifyParty(this.sandbox!.id);
    if (response?.error) {
      await MessageDialog.open(
          this.dialog,
          "Error notifying party",
          response.error);
    }
  }

  async onClickResetParty() {
    if (
      await ConfirmationDialog.open(
        this.dialog,
        "Reset party",
        "Are you sure you want to reset the party? "
        + "All current party data will be lost.")
    ) {
      const response: any = await this.conformanceService.resetParty(this.sandbox!.id);
      if (response?.error) {
        await MessageDialog.open(
            this.dialog,
            "Error reseting party",
            response.error);
      }
    }
  }

  onClickRefresh() {
    this.sandbox = undefined;
    this._loadData();
  }

  async onClickCreateReport() {
    const response: any = await this.conformanceService.createReport(this.sandbox!.id, this.newReportTitle);
    if (response?.error) {
      await MessageDialog.open(
          this.dialog,
          "Error creating report",
          response.error);
      this.newReportTitle = "";
      return
    }
    this.reportDigests = await this.conformanceService.getReportDigests(this.sandbox!.id);
    this.newReportTitle = "";
  }

  async onReportClick(reportDigest: ReportDigest) {
    const response: any = await this.conformanceService.getReportContent(this.sandbox!.id, reportDigest.isoTimestamp);
    if (response?.error) {
      await MessageDialog.open(
          this.dialog,
          "Error retrieving report content",
          response.error);
      return;
    }
    this.displayedReportContent = response;
    this.displayedReportDigest = reportDigest;
  }

  onClickBackToAllReports() {
    this.displayedReportDigest = null;
    this.displayedReportContent = null;
  }
}
