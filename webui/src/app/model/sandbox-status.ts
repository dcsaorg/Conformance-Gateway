export interface SandboxStatus {
  waiting: {
    who: string,
    forWhat: string,
  }[],
}
