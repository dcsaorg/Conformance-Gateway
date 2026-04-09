package org.dcsa.conformance.core.party;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.dcsa.conformance.core.toolkit.Url;

@Getter
@Setter
@ToString
public class CounterpartConfiguration {
  private boolean inManualMode;
  private String name;
  private String role;
  private Url url;
  private String authHeaderName = "";
  private String authHeaderValue = "";
  private HttpHeaderConfiguration[] externalPartyAdditionalHeaders;
  private EndpointUriOverrideConfiguration[] endpointUriOverrideConfigurations;
}
