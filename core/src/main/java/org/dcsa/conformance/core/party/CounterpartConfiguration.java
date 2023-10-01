package org.dcsa.conformance.core.party;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CounterpartConfiguration {
  private boolean inManualMode;
  private String name;
  private String role;
  private String baseUrl;
  private String rootPath;
}
