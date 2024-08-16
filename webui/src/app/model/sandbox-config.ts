export interface SandboxConfig {
    sandboxId: string,
    sandboxName: string,
    sandboxUrl: string,
    sandboxAuthHeaderName: string,
    sandboxAuthHeaderValue: string,
    externalPartyUrl: string,
    externalPartyAuthHeaderName: string,
    externalPartyAuthHeaderValue: string,
    externalPartyAdditionalHeaders: HeaderNameAndValue[]
}

export interface HeaderNameAndValue {
  headerName: string,
  headerValue: string,
}
