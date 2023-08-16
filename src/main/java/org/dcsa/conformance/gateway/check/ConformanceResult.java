package org.dcsa.conformance.gateway.check;

import lombok.Getter;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

import java.util.Collections;
import java.util.Set;

@Getter
public class ConformanceResult {
  private final String checkedPartyName;
  private final ConformanceExchange checkedExchange;
  private final boolean conformant;
  private final Set<String> errors;

  private ConformanceResult(
      String checkedPartyName,
      ConformanceExchange checkedExchange,
      boolean conformant,
      Set<String> errors) {
    this.checkedPartyName = checkedPartyName;
    this.checkedExchange = checkedExchange;
    this.conformant = conformant;
    this.errors = Collections.unmodifiableSet(errors);
  }

  public static ConformanceResult forSourceParty(ConformanceExchange exchange, boolean conformant) {
    return new ConformanceResult(
        exchange.getSourcePartyName(), exchange, conformant, Collections.emptySet());
  }

  public static ConformanceResult forTargetParty(ConformanceExchange exchange, boolean conformant) {
    return new ConformanceResult(
        exchange.getTargetPartyName(), exchange, conformant, Collections.emptySet());
  }

  public static ConformanceResult forSourceParty(ConformanceExchange exchange, Set<String> errors) {
    return new ConformanceResult(exchange.getSourcePartyName(), exchange, errors.isEmpty(), errors);
  }

  public static ConformanceResult forTargetParty(ConformanceExchange exchange, Set<String> errors) {
    return new ConformanceResult(exchange.getTargetPartyName(), exchange, errors.isEmpty(), errors);
  }
}
