import { ConformanceStatus } from "./conformance-status";

export interface Scenario {
    id: string,
    name: string,
    conformanceStatus: ConformanceStatus,
}
