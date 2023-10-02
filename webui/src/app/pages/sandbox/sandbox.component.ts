import { Component } from "@angular/core";
import { ConformanceService } from "../../service/conformance.service";
import { ActivatedRoute, Router } from "@angular/router";
import { AuthService } from "../../auth/auth.service";
import { Sandbox } from "../../model/sandbox";
import { Subscription } from "rxjs";
import { Scenario } from "src/app/model/scenario";
import { ConformanceStatus } from "src/app/model/conformance-status";

@Component({
  selector: 'app-sandbox',
  templateUrl: './sandbox.component.html',
  styleUrls: ['../../shared-styles.css']
})
export class SandboxComponent {

  sandbox: Sandbox | undefined;
  scenarios: Scenario[] = [];
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
        const sandboxId: string = params['sandboxId'];
        this.sandbox = await this.conformanceService.getSandbox(sandboxId);
        this.scenarios = await this.conformanceService.getSandboxScenarios(sandboxId);
      });
  }

  async ngOnDestroy() {
    if (this.activatedRouteSubscription) {
      this.activatedRouteSubscription.unsubscribe();
    }
  }

  getConformanceStatusEmoji(scenario: Scenario): string {
    switch (scenario.conformanceStatus) {
      case ConformanceStatus.CONFORMANT:
        return "✅";
      case ConformanceStatus.NON_CONFORMANT:
        return "🚫";
      case ConformanceStatus.PARTIALLY_CONFORMANT:
        return "⚠️";
      case ConformanceStatus.NO_TRAFFIC:
        return "❔";
    }
  }

  getConformanceStatusTitle(scenario: Scenario): string {
    switch (scenario.conformanceStatus) {
      case ConformanceStatus.CONFORMANT:
        return "Conformant";
      case ConformanceStatus.NON_CONFORMANT:
        return "Non-conformant";
      case ConformanceStatus.PARTIALLY_CONFORMANT:
        return "Partially conformant";
      case ConformanceStatus.NO_TRAFFIC:
        return "No traffic";
    }
  }

  getActionIconName(scenario: Scenario): string {
    switch (scenario.conformanceStatus) {
      case ConformanceStatus.CONFORMANT:
      case ConformanceStatus.NON_CONFORMANT:
      case ConformanceStatus.PARTIALLY_CONFORMANT:
        return "replay";
      case ConformanceStatus.NO_TRAFFIC:
        return "play_arrow";
    }
  }

  getActionTitle(scenario: Scenario): string {
    switch (scenario.conformanceStatus) {
      case ConformanceStatus.CONFORMANT:
      case ConformanceStatus.NON_CONFORMANT:
      case ConformanceStatus.PARTIALLY_CONFORMANT:
        return "Restart";
      case ConformanceStatus.NO_TRAFFIC:
        return "Start";
    }
  }

  async onScenarioAction(scenario: Scenario) {
    await this.conformanceService.startScenario(this.sandbox!.id, scenario.id);
    this.router.navigate([
      '/scenario', this.sandbox!.id, scenario.id
    ]);
  }

  async onScenarioClick(scenario: Scenario) {
    this.router.navigate([
      '/scenario', this.sandbox!.id, scenario.id
    ]);
  }
}
