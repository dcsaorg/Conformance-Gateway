import { Injectable } from "@angular/core";
import { ApiService } from "./api.service";
import { Sandbox } from "../model/sandbox";
import { ScenarioDigest } from "../model/scenario";
import { ScenarioStatus } from "../model/scenario-status";
import { Standard } from "../model/standard";
import { SandboxConfig } from "../model/sandbox-config";

@Injectable({
  providedIn: 'root'
})
export class ConformanceService {
  constructor(
    private apiService: ApiService,
  ) {
  }

  async getAvailableStandards(): Promise<Standard[]> {
    const standards: Standard[] = await this.apiService.call({
      operation: "getAvailableStandards",
    });
    return standards;
  }

  async getAllSandboxes(): Promise<Sandbox[]> {
    const sandboxes: Sandbox[] = await this.apiService.call({
      operation: "getAllSandboxes",
    });
    return sandboxes;
  }

  async getSandbox(sandboxId: string, includeOperatorLog: boolean): Promise<Sandbox> {
    const sandbox: Sandbox = await this.apiService.call({
      operation: "getSandbox",
      sandboxId,
      includeOperatorLog,
    });
    return sandbox;
  }

  async notifyParty(sandboxId: string): Promise<Sandbox> {
    const sandbox: Sandbox = await this.apiService.call({
      operation: "notifyParty",
      sandboxId,
    });
    return sandbox;
  }

  async getScenarioDigests(sandboxId: string): Promise<ScenarioDigest[]> {
    const scenarios: ScenarioDigest[] = await this.apiService.call({
      operation: "getScenarioDigests",
      sandboxId,
    });
    return scenarios;
  }

  async getScenario(sandboxId: string, scenarioId: string): Promise<ScenarioDigest> {
    const scenario: ScenarioDigest = await this.apiService.call({
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

  async startOrStopScenario(sandboxId: string, scenarioId: string): Promise<void> {
    await this.apiService.call({
      operation: "startOrStopScenario",
      sandboxId,
      scenarioId,
    });
  }

  async createSandbox(
    standardName: string,
    versionNumber: string,
    testedPartyRole: string,
    isDefaultType: boolean,
    sandboxName: string
  ): Promise<string> {
    const reply: { sandboxId: string } = await this.apiService.call({
      operation: "createSandbox",
      standardName,
      versionNumber,
      testedPartyRole,
      isDefaultType,
      sandboxName,
    });
    return reply.sandboxId;
  }

  async getSandboxConfig(sandboxId: string): Promise<SandboxConfig> {
    const sandboxConfig: SandboxConfig = await this.apiService.call({
      operation: "getSandboxConfig",
      sandboxId
    });
    return sandboxConfig;
  }

  async updateSandboxConfig(
    sandboxId: string,
    sandboxName: string,
    externalPartyUrl: string,
    externalPartyAuthHeaderName: string,
    externalPartyAuthHeaderValue: string,
  ): Promise<void> {
    await this.apiService.call({
      operation: "updateSandboxConfig",
      sandboxId,
      sandboxName,
      externalPartyUrl,
      externalPartyAuthHeaderName,
      externalPartyAuthHeaderValue,
    });
  }
}
