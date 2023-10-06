package org.dcsa.conformance.core.party;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PartyConfiguration {
  private boolean inManualMode;
  private String name;
  private String role;
  private String orchestratorUrl;
}
