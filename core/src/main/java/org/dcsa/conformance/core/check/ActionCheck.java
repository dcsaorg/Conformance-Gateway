package org.dcsa.conformance.core.check;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

@Slf4j
public abstract class ActionCheck extends ConformanceCheck {

  private final Predicate<String> isRelevantForRoleName;
  protected final UUID matchedExchangeUuid;
  protected final HttpMessageType httpMessageType;

  protected ActionCheck(
      String title,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType) {
    this("", title, isRelevantForRoleName, matchedExchangeUuid, httpMessageType);
  }

  protected ActionCheck(
      String titlePrefix,
      String title,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType) {
    super(titlePrefix + (titlePrefix.endsWith(" ") ? "" : " ") + title);
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
    try {
      ConformanceCheckResult result = performCheck(getExchangeByUuid);
      addResult(getExchangeByUuid, result);
    } catch (Exception e) {
      String errorMessage = buildErrorMessage(e);
      log.warn(errorMessage, e);
      addResult(getExchangeByUuid, ConformanceCheckResult.simple(Set.of(errorMessage)));
    }
  }

  protected abstract ConformanceCheckResult performCheck(
      Function<UUID, ConformanceExchange> getExchangeByUuid);

  public ActionCheck withRelevance(boolean isRelevant) {
    this.setRelevant(isRelevant);
    return this;
  }

  private void addResult(
      Function<UUID, ConformanceExchange> getExchangeByUuid, ConformanceCheckResult result) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return;

    switch (result) {
      case ConformanceCheckResult.SimpleErrors(var errors) ->
          this.addResult(ConformanceResult.withErrors(errors));
      case ConformanceCheckResult.ErrorsWithRelevance(var errors) ->
          this.addResult(ConformanceResult.withErrorsAndRelevance(errors));
    }
  }

  private String buildErrorMessage(Exception e) {
    String message = "Failed to perform ActionCheck '%s'".formatted(title);
    if (e instanceof UserFacingException) {
      message += ": %s".formatted(e.getMessage());
    }
    return message;
  }
}
