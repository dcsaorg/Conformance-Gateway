import {Injectable} from "@angular/core";
import {ApiService} from "./api.service";
import {Sandbox} from "../model/sandbox";
import {ScenarioDigest} from "../model/scenario";
import {ScenarioStatus} from "../model/scenario-status";
import {Standard} from "../model/standard";
import {HeaderNameAndValue, SandboxConfig} from "../model/sandbox-config";
import {StandardModule} from "../model/standard-module";
import {SandboxStatus} from "../model/sandbox-status";

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

  async getSandboxStatus(sandboxId: string): Promise<SandboxStatus> {
    const sandboxStatus: SandboxStatus = await this.apiService.call({
      operation: "getSandboxStatus",
      sandboxId,
    });
    return sandboxStatus;
  }

  async notifyParty(sandboxId: string): Promise<Sandbox> {
    const sandbox: Sandbox = await this.apiService.call({
      operation: "notifyParty",
      sandboxId,
    });
    return sandbox;
  }

  async resetParty(sandboxId: string): Promise<Sandbox> {
    const sandbox: Sandbox = await this.apiService.call({
      operation: "resetParty",
      sandboxId,
    });
    return sandbox;
  }

  async getScenarioDigests(sandboxId: string): Promise<StandardModule[]> {
    return await this.apiService.call({
      operation: "getScenarioDigests",
      sandboxId,
    });
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

  async handleActionInput(sandboxId: string, scenarioId: string, actionId: string, actionInput: any | string | undefined): Promise<any> {
    return await this.apiService.call({
      operation: "handleActionInput",
      sandboxId,
      scenarioId,
      actionId,
      actionInput,
    });
  }

  async startOrStopScenario(sandboxId: string, scenarioId: string): Promise<void> {
    await this.apiService.call({
      operation: "startOrStopScenario",
      sandboxId,
      scenarioId,
    });
  }

  async completeCurrentAction(sandboxId: string): Promise<any> {
    return await this.apiService.call({
      operation: "completeCurrentAction",
      sandboxId,
    });
  }

  async createSandbox(
    standardName: string,
    versionNumber: string,
    scenarioSuite: string,
    testedPartyRole: string,
    isDefaultType: boolean,
    sandboxName: string
  ): Promise<string> {
    const reply: { sandboxId: string } = await this.apiService.call({
      operation: "createSandbox",
      standardName,
      versionNumber,
      scenarioSuite,
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
    externalPartyAdditionalHeaders: HeaderNameAndValue[],
  ): Promise<any> {
    return await this.apiService.call({
      operation: "updateSandboxConfig",
      sandboxId,
      sandboxName,
      externalPartyUrl,
      externalPartyAuthHeaderName,
      externalPartyAuthHeaderValue,
      externalPartyAdditionalHeaders,
    });
  }
}
