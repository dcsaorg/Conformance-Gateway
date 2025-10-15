package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public class ComplexValidatorCheck extends ActionCheck {

  private final String standardsVersion;
  private final JsonComplexContentCheck validator;

  protected ComplexValidatorCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      String standardsVersion,
      JsonComplexContentCheck validator) {
    super(validator.description(), isRelevantForRoleName, matchedExchangeUuid, httpMessageType);
    this.validator = validator;
    this.standardsVersion = standardsVersion;
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Set<ConformanceError> checkConformanceAndRelevance(
      Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return Collections.emptySet();
    JsonNode jsonBody = exchange.getMessage(httpMessageType).body().getJsonBody();
    return VersionedKeywordDataset.withVersion(
        standardsVersion, () -> this.validator.validate(jsonBody));
  }
}
