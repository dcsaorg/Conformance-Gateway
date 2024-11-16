package org.dcsa.conformance.core.party;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class EndpointUriOverrideConfiguration {
  private String method;
  private String endpointBaseUri;
  private String endpointSuffix;
  private String baseUriOverride;
}
