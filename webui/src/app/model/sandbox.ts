export interface Sandbox {
  id: string,
  name: string,
  standardName: string,
  standardVersion: string,
  scenarioSuite: string,
  testedPartyRole: string,
  isDefault: boolean,
  canNotifyParty: boolean | null | undefined,
  operatorLog: string[] | null | undefined,
}
