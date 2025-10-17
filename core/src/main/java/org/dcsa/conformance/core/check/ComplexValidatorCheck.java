package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

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
  protected ConformanceCheckResult performCheck(
      Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return ConformanceCheckResult.withRelevance(Collections.emptySet());
    JsonNode jsonBody = exchange.getMessage(httpMessageType).body().getJsonBody();
    return ConformanceCheckResult.withRelevance(
        VersionedKeywordDataset.withVersion(
            standardsVersion, () -> this.validator.validate(jsonBody)));
  }
}
