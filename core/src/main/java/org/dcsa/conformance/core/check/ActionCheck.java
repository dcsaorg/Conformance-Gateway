package org.dcsa.conformance.core.check;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

@Slf4j
public abstract class ActionCheck extends ConformanceCheck {
  private final Predicate<String> isRelevantForRoleName;
  protected final UUID matchedExchangeUuid;
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
  protected final void doCheck(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    Set<String> conformanceErrors;
    try {
      conformanceErrors = checkConformance(getExchangeByUuid);
    } catch (Exception e) {
      String message = "Failed to perform ActionCheck: " + title;
      log.warn(message, e);
      conformanceErrors = Set.of(message);
    }
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange != null) {
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
  }

  protected abstract Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid);
}
