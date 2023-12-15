package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.NonNull;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

class JsonAttributeBasedCheck extends ActionCheck {

  private final List<JsonContentCheck> validators;

  JsonAttributeBasedCheck(
    String titlePrefix,
    String title,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    @NonNull
    List<@NonNull JsonContentCheck> validators) {
    super(titlePrefix, title, isRelevantForRoleName, matchedExchangeUuid, httpMessageType);
    if (validators.isEmpty()) {
      throw new IllegalArgumentException("Must have at least one subcheck (validators must be non-empty)");
    }
    this.validators = validators;
  }

  @Override
  protected final Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    // All checks are delegated to sub-checks; nothing to do in here.
    return Collections.emptySet();
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return this.validators.stream()
      .map(validator -> new SingleValidatorCheck(this::isRelevantForRole, matchedExchangeUuid, httpMessageType, validator));
  }


  private static class SingleValidatorCheck extends ActionCheck {

    private final JsonContentCheck validator;

    public SingleValidatorCheck(Predicate<String> isRelevantForRoleName, UUID matchedExchangeUuid, HttpMessageType httpMessageType, @NonNull JsonContentCheck validator) {
      super(validator.description(), isRelevantForRoleName, matchedExchangeUuid, httpMessageType);
      this.validator = validator;
    }

    @Override
    protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
      ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
      if (exchange == null) return Collections.emptySet();
      JsonNode jsonBody = exchange.getMessage(httpMessageType).body().getJsonBody();
      return this.validator.validate(jsonBody);
    }
  }
}
