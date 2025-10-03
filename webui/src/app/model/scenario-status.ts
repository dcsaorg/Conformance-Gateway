import { ConformanceStatus } from "./conformance-status"

export interface ScenarioStatus {
    isRunning: boolean,
    nextActions: string,
    promptText: string,
    jsonForPromptText: any,
    promptActionId: string,
    confirmationRequired: boolean,
    inputRequired: boolean,
    conformanceSubReport: ScenarioConformanceReport,
    isSkippable: boolean,
    needsAction: boolean,
}

export interface ScenarioConformanceReport {
    title: string,
    status: ConformanceStatus,
    errorMessages: string[],
    subReports: ScenarioConformanceReport[],
}
