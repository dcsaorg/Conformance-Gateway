import {ScenarioDigest} from "./scenario";

export interface StandardModule {
  moduleName: string,
  scenarios: ScenarioDigest[],
}
