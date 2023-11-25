package org.dcsa.conformance.core.check;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class JsonSchemaCheck extends ActionCheck {
  private final HttpMessageType httpMessageType;
  private final JsonSchemaValidator jsonSchemaValidator;

  public JsonSchemaCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      JsonSchemaValidator jsonSchemaValidator) {
    this("", isRelevantForRoleName, matchedExchangeUuid, httpMessageType, jsonSchemaValidator);
  }

  public JsonSchemaCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      JsonSchemaValidator jsonSchemaValidator) {
    super(
        titlePrefix,
        "The HTTP %s matches the standard JSON schema"
            .formatted(httpMessageType.name().toLowerCase()),
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType);
    this.httpMessageType = httpMessageType;
    this.jsonSchemaValidator = jsonSchemaValidator;
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    return exchange == null
        ? Collections.emptySet()
        : jsonSchemaValidator.validate(exchange.getMessage(httpMessageType).body().getStringBody());
  }
}
