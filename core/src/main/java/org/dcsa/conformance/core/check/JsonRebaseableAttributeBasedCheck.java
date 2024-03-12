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

class JsonRebaseableAttributeBasedCheck extends ActionCheck {

  private final JsonContentCheckRebaser rebaser;
  private final List<JsonRebaseableContentCheck> validators;

  JsonRebaseableAttributeBasedCheck(
    String titlePrefix,
    String title,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    @NonNull JsonContentCheckRebaser rebaser,
    @NonNull
    List<@NonNull JsonRebaseableContentCheck> validators) {
    super(titlePrefix, title, isRelevantForRoleName, matchedExchangeUuid, httpMessageType);
    if (validators.isEmpty()) {
      throw new IllegalArgumentException("Must have at least one subcheck (validators must be non-empty)");
    }
    this.rebaser = rebaser;
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
      .map(validator -> new SingleValidatorCheck(this::isRelevantForRole, matchedExchangeUuid, httpMessageType, rebaser.offset(validator)));
  }


  private static class SingleValidatorCheck extends ActionCheck {

    private final JsonRebaseableContentCheck validator;

    public SingleValidatorCheck(Predicate<String> isRelevantForRoleName, UUID matchedExchangeUuid, HttpMessageType httpMessageType, @NonNull JsonRebaseableContentCheck validator) {
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