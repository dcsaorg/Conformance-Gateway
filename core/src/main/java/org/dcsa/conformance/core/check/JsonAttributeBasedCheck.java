package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.NonNull;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

class JsonAttributeBasedCheck extends ActionCheck {

  private final String standardsVersion;
  private final List<JsonContentCheck> validators;

  JsonAttributeBasedCheck(
      String titlePrefix,
      String title,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      String standardsVersion,
      @NonNull List<@NonNull JsonContentCheck> validators) {
    super(titlePrefix, title, isRelevantForRoleName, matchedExchangeUuid, httpMessageType);
    if (validators.isEmpty()) {
      throw new IllegalArgumentException(
          "Must have at least one subcheck (validators must be non-empty)");
    }
    this.standardsVersion = standardsVersion;
    if (this.standardsVersion == null) {
      throw new IllegalArgumentException();
    }
    this.validators = validators;
  }

  @Override
  protected final ConformanceCheckResult performCheck(
      Function<UUID, ConformanceExchange> getExchangeByUuid) {
    // All checks are delegated to sub-checks; nothing to do in here.
    return ConformanceCheckResult.simple(Collections.emptySet());
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return this.validators.stream()
        .map(
            validator ->
                new SingleValidatorCheck(
                    this::isRelevantForRole,
                    matchedExchangeUuid,
                    httpMessageType,
                    standardsVersion,
                    validator));
  }

  private static class SingleValidatorCheck extends ActionCheck {

    private final String standardsVersion;
    private final JsonContentCheck validator;

    public SingleValidatorCheck(
        Predicate<String> isRelevantForRoleName,
        UUID matchedExchangeUuid,
        HttpMessageType httpMessageType,
        String standardsVersion,
        @NonNull JsonContentCheck validator) {
      super(validator.description(), isRelevantForRoleName, matchedExchangeUuid, httpMessageType);
      this.standardsVersion = standardsVersion;
      this.validator = validator;
      this.setRelevant(validator.isRelevant());
    }

    @Override
    protected ConformanceCheckResult performCheck(
        Function<UUID, ConformanceExchange> getExchangeByUuid) {
      ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
      if (exchange == null) return ConformanceCheckResult.simple(Collections.emptySet());
      if (exchange.getResponse().statusCode() == 202) this.setApplicable(false);
      JsonNode jsonBody = exchange.getMessage(httpMessageType).body().getJsonBody();
      return VersionedKeywordDataset.withVersion(
          standardsVersion, () -> this.validator.validate(jsonBody));
    }
  }
}
