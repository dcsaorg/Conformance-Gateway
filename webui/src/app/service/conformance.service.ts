import { Injectable } from "@angular/core";
import { ApiService } from "./api.service";
import { Sandbox } from "../model/sandbox";
import { Scenario } from "../model/scenario";
import { ScenarioStatus } from "../model/scenario-status";

@Injectable({
  providedIn: 'root'
})
export class ConformanceService {
  constructor(
    private apiService: ApiService,
  ) {
  }

  async getAllSandboxes(): Promise<Sandbox[]> {
    const sandboxes: Sandbox[] = await this.apiService.call({
      operation: "getAllSandboxes",
    });
    return sandboxes;
  }

  async getSandbox(sandboxId: string): Promise<Sandbox> {
    const sandbox: Sandbox = await this.apiService.call({
      operation: "getSandbox",
      sandboxId,
    });
    return sandbox;
  }

  async getSandboxScenarios(sandboxId: string): Promise<Scenario[]> {
    const scenarios: Scenario[] = await this.apiService.call({
      operation: "getSandboxScenarios",
      sandboxId,
    });
    return scenarios;
  }

  async getScenario(sandboxId: string, scenarioId: string): Promise<Scenario> {
    const scenario: Scenario = await this.apiService.call({
      operation: "getScenario",
      sandboxId,
      scenarioId,
    });
    return scenario;
  }

  async getScenarioStatus(sandboxId: string, scenarioId: string): Promise<ScenarioStatus> {
    const scenarioStatus: ScenarioStatus = await this.apiService.call({
      operation: "getScenarioStatus",
      sandboxId,
      scenarioId,
    });
    return scenarioStatus;
  }

  async handleActionInput(sandboxId: string, scenarioId: string, actionId: string, actionInput: string | undefined): Promise<ScenarioStatus> {
    const scenarioStatus: ScenarioStatus = await this.apiService.call({
      operation: "handleActionInput",
      sandboxId,
      scenarioId,
      actionId,
      actionInput,
    });
    return scenarioStatus;
  }

  async startScenario(sandboxId: string, scenarioId: string): Promise<void> {
    await this.apiService.call({
      operation: "startScenario",
      sandboxId,
      scenarioId,
    });
  }
}
