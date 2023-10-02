package org.dcsa.conformance.core.check;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public abstract class ActionCheck extends ConformanceCheck {
  private final Predicate<String> isRelevantForRoleName;
  private final UUID matchedExchangeUuid;
  protected final HttpMessageType httpMessageType;

  public ActionCheck(
      String title,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType) {
    super(title);
    this.isRelevantForRoleName = isRelevantForRoleName;
    this.matchedExchangeUuid = matchedExchangeUuid;
    this.httpMessageType = httpMessageType;
  }

  @Override
  public boolean isRelevantForRole(String roleName) {
    return isRelevantForRoleName.test(roleName);
  }

  @Override
  protected final void doCheck(ConformanceExchange exchange) {
    if (!exchange.getUuid().equals(matchedExchangeUuid)) return;

    final Set<String> conformanceErrors;
    try {
      conformanceErrors = checkConformance(exchange);
    } catch (Exception e) {
      throw new RuntimeException("Failed to perform ActionCheck: " + title, e);
    }
    switch (httpMessageType) {
      case REQUEST -> {
        if (isRelevantForRole(exchange.getRequest().message().sourcePartyRole()))
          this.addResult(ConformanceResult.forSourceParty(exchange, conformanceErrors));
      }
      case RESPONSE -> {
        if (isRelevantForRole(exchange.getRequest().message().targetPartyRole()))
          this.addResult(ConformanceResult.forTargetParty(exchange, conformanceErrors));
      }
    }
  }

  protected abstract Set<String> checkConformance(ConformanceExchange exchange);
}
