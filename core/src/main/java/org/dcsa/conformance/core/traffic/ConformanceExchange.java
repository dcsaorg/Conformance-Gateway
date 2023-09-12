package org.dcsa.conformance.core.traffic;

import java.util.UUID;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class ConformanceExchange {
  private final UUID uuid;
  private final ConformanceRequest request;
  private final ConformanceResponse response;

  public ConformanceExchange(ConformanceRequest request, ConformanceResponse response) {
    this.uuid = UUID.randomUUID();
    this.request = request;
    this.response = response;
  }
}
