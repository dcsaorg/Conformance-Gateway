import { ConformanceStatus } from "./conformance-status"

export interface ScenarioStatus {
    isRunning: boolean,
    nextActions: string,
    promptText: string,
    promptActionId: string,
    confirmationRequired: boolean,
    inputRequired: boolean,
    conformanceSubReport: ScenarioConformanceReport
}

export interface ScenarioConformanceReport {
    title: string,
    status: ConformanceStatus,
    errorMessages: string[],
    subReports: ScenarioConformanceReport[],
}
