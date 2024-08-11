export interface SandboxStatus {
  waiting: SandboxWaiting[],
}

export interface SandboxWaiting {
  who: string,
  forWhom: string,
  toDoWhat: string,
}
