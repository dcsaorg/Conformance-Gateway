package org.dcsa.conformance.core.party;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndpointUriOverrideConfiguration {
  private String method;
  private String endpointBaseUri;
  private String endpointSuffix;
  private String baseUriOverride;
}
