package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

class JsonAttributeBasedCheck extends ActionCheck {

  private final Function<JsonNode, Set<String>> validator;

  JsonAttributeBasedCheck(
    String titlePrefix,
    String title,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    @NonNull
    Function<JsonNode, Set<String>> validator) {
    super(titlePrefix, title, isRelevantForRoleName, matchedExchangeUuid, httpMessageType);
    this.validator = validator;
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return Collections.emptySet();
    JsonNode jsonBody = exchange.getMessage(httpMessageType).body().getJsonBody();
    return this.validator.apply(jsonBody);
  }

}
