package org.dcsa.conformance.core.check;

import java.util.*;
import lombok.Getter;
import org.dcsa.conformance.core.traffic.ConformanceExchange;

@Getter
public class ConformanceResult {
  private final String checkedPartyName;
  private final LinkedList<ConformanceExchange> checkedExchanges;
  private final boolean conformant;
  private final Set<String> errors;

  private ConformanceResult(
      String checkedPartyName,
      ConformanceExchange checkedExchange,
      boolean conformant,
      Set<String> errors) {
    this.checkedPartyName = checkedPartyName;
    this.checkedExchanges = new LinkedList<>(List.of(checkedExchange));
    this.conformant = conformant;
    this.errors = Collections.unmodifiableSet(errors);
  }

  public static ConformanceResult forSourceParty(ConformanceExchange exchange, Set<String> errors) {
    return new ConformanceResult(
        exchange.getRequest().message().sourcePartyName(), exchange, errors.isEmpty(), errors);
  }

  public static ConformanceResult forTargetParty(ConformanceExchange exchange, Set<String> errors) {
    return new ConformanceResult(
        exchange.getRequest().message().targetPartyName(), exchange, errors.isEmpty(), errors);
  }

}
