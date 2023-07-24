package org.dcsa.conformance.gateway.check;

import lombok.Getter;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

public class ConformanceResult {
  @Getter private String checkedPartyName;
  @Getter private ConformanceExchange checkedExchange;
  @Getter private boolean conformant;

  private ConformanceResult(
      String checkedPartyName,
      ConformanceExchange checkedExchange,
      boolean conformant) {
    this.checkedPartyName = checkedPartyName;
    this.checkedExchange = checkedExchange;
    this.conformant = conformant;
  }

  public static ConformanceResult forSourceParty(
      ConformanceExchange exchange, boolean conformant) {
    return new ConformanceResult(exchange.getSourcePartyName(), exchange, conformant);
  }

  public static ConformanceResult forTargetParty(
      ConformanceExchange exchange, boolean conformant) {
    return new ConformanceResult(exchange.getTargetPartyName(), exchange, conformant);
  }
}
