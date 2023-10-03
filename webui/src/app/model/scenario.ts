import { ConformanceStatus } from "./conformance-status";

export interface ScenarioDigest {
    id: string,
    name: string,
    isRunning: string,
    conformanceStatus: ConformanceStatus,
}
