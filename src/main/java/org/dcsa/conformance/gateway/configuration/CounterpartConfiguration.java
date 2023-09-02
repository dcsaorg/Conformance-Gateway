package org.dcsa.conformance.gateway.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CounterpartConfiguration {
  private String name;
  private String role;
  private String baseUrl;
  private String rootPath;
}
