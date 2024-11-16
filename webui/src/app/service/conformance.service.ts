import {Injectable} from "@angular/core";
import {ApiService} from "./api.service";
import {Sandbox} from "../model/sandbox";
import {ScenarioDigest} from "../model/scenario";
import {ScenarioStatus} from "../model/scenario-status";
import {Standard} from "../model/standard";
import {EndpointUriOverride, HeaderNameAndValue, SandboxConfig} from "../model/sandbox-config";
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
    return await this.apiService.call({
      operation: "getAvailableStandards",
    });
  }

  async getAllSandboxes(): Promise<Sandbox[]> {
    return await this.apiService.call({
      operation: "getAllSandboxes",
    });
  }

  async getSandbox(sandboxId: string, includeOperatorLog: boolean): Promise<Sandbox> {
    return await this.apiService.call({
      operation: "getSandbox",
      sandboxId,
      includeOperatorLog,
    });
  }

  async getSandboxStatus(sandboxId: string): Promise<SandboxStatus> {
    return await this.apiService.call({
      operation: "getSandboxStatus",
      sandboxId,
    });
  }

  async notifyParty(sandboxId: string): Promise<Sandbox> {
    return await this.apiService.call({
      operation: "notifyParty",
      sandboxId,
    });
  }

  async resetParty(sandboxId: string): Promise<Sandbox> {
    return await this.apiService.call({
      operation: "resetParty",
      sandboxId,
    });
  }

  async getScenarioDigests(sandboxId: string): Promise<StandardModule[]> {
    return await this.apiService.call({
      operation: "getScenarioDigests",
      sandboxId,
    });
  }

  async getScenario(sandboxId: string, scenarioId: string): Promise<ScenarioDigest> {
    return await this.apiService.call({
      operation: "getScenario",
      sandboxId,
      scenarioId,
    });
  }

  async getScenarioStatus(sandboxId: string, scenarioId: string): Promise<ScenarioStatus> {
    return await this.apiService.call({
      operation: "getScenarioStatus",
      sandboxId,
      scenarioId,
    });
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
    return await this.apiService.call({
      operation: "getSandboxConfig",
      sandboxId
    });
  }

  async updateSandboxConfig(
    sandboxId: string,
    sandboxName: string,
    externalPartyUrl: string,
    externalPartyAuthHeaderName: string,
    externalPartyAuthHeaderValue: string,
    externalPartyAdditionalHeaders: HeaderNameAndValue[],
    externalPartyEndpointUriOverrides: EndpointUriOverride[] | undefined,
  ): Promise<any> {
    return await this.apiService.call({
      operation: "updateSandboxConfig",
      sandboxId,
      sandboxName,
      externalPartyUrl,
      externalPartyAuthHeaderName,
      externalPartyAuthHeaderValue,
      externalPartyAdditionalHeaders,
      externalPartyEndpointUriOverrides,
    });
  }
}
