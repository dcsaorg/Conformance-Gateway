export interface SandboxConfig {
  sandboxId: string,
  sandboxName: string,
  sandboxUrl: string,
  sandboxEndpointUriMethods: EndpointUriMethods[],
  sandboxAuthHeaderName: string,
  sandboxAuthHeaderValue: string,
  externalPartyUrl: string,
  externalPartyEndpointUriMethods: EndpointUriMethods[],
  externalPartyEndpointUriOverrides: EndpointUriOverride[] | undefined,
  externalPartyAuthHeaderName: string,
  externalPartyAuthHeaderValue: string,
  externalPartyAdditionalHeaders: HeaderNameAndValue[]
}

export interface EndpointUriMethods {
  endpointUri: string,
  methods: string[]
}

export interface EndpointUriOverride {
  method: string,
  endpointBaseUri: string,
  endpointSuffix: string,
  baseUriOverride: string,
}

export interface HeaderNameAndValue {
  headerName: string,
  headerValue: string,
}
