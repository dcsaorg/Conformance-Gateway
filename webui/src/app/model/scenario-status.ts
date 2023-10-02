export interface ScenarioStatus {
    runId: string,
    nextActions: string,
    promptText: string,
    promptActionId: string,
    confirmationRequired: boolean,
    inputRequired: boolean,
}
