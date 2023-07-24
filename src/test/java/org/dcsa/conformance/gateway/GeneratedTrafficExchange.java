package org.dcsa.conformance.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
public class GeneratedTrafficExchange {
  @Getter private String link;
  @Getter private String requestPath;
  @Getter private String requestBody;
  @Getter private String responseBody;
}
