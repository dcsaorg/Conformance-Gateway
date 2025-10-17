package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.ConformanceError;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.util.JsonUtil;

public abstract class PayloadContentConformanceCheck extends ActionCheck {

  protected static final String UNSET_MARKER = "<unset>";

  protected PayloadContentConformanceCheck(
      String title,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType) {
    super("[Notification]", title, isRelevantForRoleName, matchedExchangeUuid, httpMessageType);
  }

  @Override
  protected final Set<String> checkConformance(
      Function<UUID, ConformanceExchange> getExchangeByUuid) {
    // All checks are delegated to sub-checks; nothing to do in here.
    return Collections.emptySet();
  }

  @Override
  protected abstract Stream<? extends ConformanceCheck> createSubChecks();

  protected Function<JsonNode, Set<String>> at(
      String path, Function<JsonNode, Set<String>> subCheck) {
    // Eagerly compile to the pointer to weed out syntax errors early.
    var pointer = JsonPointer.compile(path);
    return payload -> subCheck.apply(payload.at(pointer));
  }

  protected ConformanceCheck createSubCheck(
      String prefix,
      String subtitle,
      boolean isRelevant,
      String path,
      Function<JsonNode, Set<String>> subCheck) {
    return new ActionCheck(
        prefix, subtitle, this::isRelevantForRole, this.matchedExchangeUuid, this.httpMessageType) {

      @Override
      public boolean isRelevant() {
        return isRelevant;
      }

      @Override
      protected Set<String> checkConformance(
          Function<UUID, ConformanceExchange> getExchangeByUuid) {
        ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
        if (exchange == null) return Collections.emptySet();
        var conformanceMessage =
            this.httpMessageType == HttpMessageType.RESPONSE
                ? exchange.getResponse().message()
                : exchange.getRequest().message();
        var payload = conformanceMessage.body().getJsonBody();
        var pointer = JsonPointer.compile(path);
        if (JsonUtil.isMissingOrEmpty(payload.at(pointer))) {
          this.setApplicable(false);
          return Set.of();
        }
        return subCheck.apply(payload);
      }
    };
  }

  protected ConformanceCheck createSubCheck(
      String prefix, String subtitle, String path, Function<JsonNode, Set<String>> subCheck) {
    return createSubCheck(prefix, subtitle, true, path, subCheck);
  }

  protected Function<JsonNode, Set<ConformanceError>> conditionalAt(
      String path, Function<JsonNode, Set<ConformanceError>> subCheck) {
    var pointer = JsonPointer.compile(path);
    return payload -> {
      JsonNode nodeAtPath = payload.at(pointer);
      return subCheck.apply(nodeAtPath);
    };
  }

  protected ConformanceCheck createConditionalSubCheck(
      String prefix,
      String subtitle,
      String path,
      Function<JsonNode, Set<ConformanceError>> subCheck) {
    return new ActionCheck(
        prefix, subtitle, this::isRelevantForRole, this.matchedExchangeUuid, this.httpMessageType) {

      @Override
      protected Set<String> checkConformance(
          Function<UUID, ConformanceExchange> getExchangeByUuid) {
        throw new UnsupportedOperationException();
      }

      @Override
      protected Set<ConformanceError> checkConformanceAndRelevance(
          Function<UUID, ConformanceExchange> getExchangeByUuid) {
        ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
        if (exchange == null) return Collections.emptySet();
        var conformanceMessage =
            this.httpMessageType == HttpMessageType.RESPONSE
                ? exchange.getResponse().message()
                : exchange.getRequest().message();
        var payload = conformanceMessage.body().getJsonBody();
        var pointer = JsonPointer.compile(path);
        if (JsonUtil.isMissingOrEmpty(payload.at(pointer))) {
          this.setApplicable(false);
          return Set.of();
        }
        return subCheck.apply(payload);
      }
    };
  }
}
