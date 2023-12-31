package org.dcsa.conformance.core.check;

import lombok.Getter;
import org.dcsa.conformance.core.traffic.ConformanceExchange;

import java.util.*;

@Getter
public class ConformanceResult {
  private final String checkedPartyName;
  private final LinkedList<ConformanceExchange> checkedExchanges;
  private final boolean conformant;
  private final Set<String> errors;

  private ConformanceResult(
      String checkedPartyName,
      LinkedList<ConformanceExchange> checkedExchanges,
      boolean conformant,
      Set<String> errors) {
    this.checkedPartyName = checkedPartyName;
    this.checkedExchanges = checkedExchanges;
    this.conformant = conformant;
    this.errors = Collections.unmodifiableSet(errors);
  }

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

  public static ConformanceResult forSourceParty(ConformanceExchange exchange, boolean conformant) {
    return new ConformanceResult(
        exchange.getRequest().message().sourcePartyName(), exchange, conformant, Collections.emptySet());
  }

  public static ConformanceResult forTargetParty(ConformanceExchange exchange, boolean conformant) {
    return new ConformanceResult(
        exchange.getRequest().message().targetPartyName(), exchange, conformant, Collections.emptySet());
  }

  public static ConformanceResult forSourceParty(ConformanceExchange exchange, Set<String> errors) {
    return new ConformanceResult(
        exchange.getRequest().message().sourcePartyName(), exchange, errors.isEmpty(), errors);
  }

  public static ConformanceResult forTargetParty(ConformanceExchange exchange, Set<String> errors) {
    return new ConformanceResult(
        exchange.getRequest().message().targetPartyName(), exchange, errors.isEmpty(), errors);
  }

  public static ConformanceResult forSourceParty(
      LinkedList<ConformanceExchange> exchanges, boolean conformant) {
    return new ConformanceResult(
        Objects.requireNonNull(exchanges.peekLast()).getRequest().message().sourcePartyName(),
        exchanges,
        conformant,
        Collections.emptySet());
  }

  public static ConformanceResult forTargetParty(
      LinkedList<ConformanceExchange> exchanges, boolean conformant) {
    return new ConformanceResult(
        Objects.requireNonNull(exchanges.peekLast()).getRequest().message().targetPartyName(),
        exchanges,
        conformant,
        Collections.emptySet());
  }

  public static ConformanceResult forSourceParty(
      LinkedList<ConformanceExchange> exchanges, Set<String> errors) {
    return new ConformanceResult(
        Objects.requireNonNull(exchanges.peekLast()).getRequest().message().sourcePartyName(),
        exchanges,
        errors.isEmpty(),
        errors);
  }

  public static ConformanceResult forTargetParty(
      LinkedList<ConformanceExchange> exchanges, Set<String> errors) {
    return new ConformanceResult(
        Objects.requireNonNull(exchanges.peekLast()).getRequest().message().targetPartyName(),
        exchanges,
        errors.isEmpty(),
        errors);
  }
}
