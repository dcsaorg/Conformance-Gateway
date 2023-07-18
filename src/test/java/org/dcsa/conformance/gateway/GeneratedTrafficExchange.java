package org.dcsa.conformance.gateway;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class GeneratedTrafficExchange {
  @Getter private String requestPath;
  @Getter private String requestBody;
  @Getter private String responseBody;
}
