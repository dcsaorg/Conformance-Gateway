import { ConformanceStatus } from "./conformance-status";

export type ScenarioConformanceType = "REQUIRED" | "OPTIONAL" | "INTERCHANGEABLE";

export interface ScenarioDigest {
    id: string,
    name: string,
    isRunning: string,
    conformanceStatus: ConformanceStatus,
    conformanceType: ScenarioConformanceType,
}
