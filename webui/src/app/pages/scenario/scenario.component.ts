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

@Component({
  selector: 'app-scenario',
  templateUrl: './scenario.component.html',
  styleUrls: ['../../shared-styles.css']
})
export class ScenarioComponent {

  sandbox: Sandbox | undefined;
  scenario: ScenarioDigest | undefined;
  scenarioStatus: ScenarioStatus | undefined;
  actionInput: string = '';
  activatedRouteSubscription: Subscription | undefined;

  getConformanceStatusEmoji = getConformanceStatusEmoji;
  getConformanceStatusTitle = getConformanceStatusTitle;

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
        const scenarioId: string = params['scenarioId'];
        this.sandbox = await this.conformanceService.getSandbox(sandboxId, false);
        this.scenario = await this.conformanceService.getScenario(sandboxId, scenarioId);
        await this.loadScenarioStatus();
      });
  }

  async loadScenarioStatus() {
    this.actionInput = '';
    this.scenarioStatus = await this.conformanceService.getScenarioStatus(
      this.sandbox!.id,
      this.scenario!.id
    );
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
    await this.conformanceService.handleActionInput(
      this.sandbox!.id,
      this.scenario!.id,
      this.scenarioStatus!.promptActionId,
      withInput ? (this.scenarioStatus?.jsonForPromptText ? JSON.parse(this.actionInput.trim()) : this.actionInput.trim()) : undefined);
    await this.loadScenarioStatus();
  }
}
