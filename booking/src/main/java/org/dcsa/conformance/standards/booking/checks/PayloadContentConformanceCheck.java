package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class PayloadContentConformanceCheck extends ActionCheck {

  protected static final Function<ObjectNode, Set<String>> ALL_OK = unused -> Collections.emptySet();

  protected static final String UNSET_MARKER = "<unset>";

  protected PayloadContentConformanceCheck(String title, Predicate<String> isRelevantForRoleName, UUID matchedExchangeUuid, HttpMessageType httpMessageType) {
    super(title, isRelevantForRoleName, matchedExchangeUuid, httpMessageType);
  }

  @Override
  protected final Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    // All checks are delegated to sub-checks; nothing to do in here.
    return Collections.emptySet();
  }

  @Override
  protected abstract Stream<? extends ConformanceCheck> createSubChecks();


  protected void addSubCheck(String subtitle, Function<ObjectNode, Set<String>> subCheck, Consumer<ConformanceCheck> addCheck) {
    addCheck.accept(createSubCheck(subtitle, subCheck));
  }

  protected ConformanceCheck createSubCheck(String subtitle, Function<ObjectNode, Set<String>> subCheck) {
    return new ActionCheck(subtitle, this::isRelevantForRole, this.matchedExchangeUuid, this.httpMessageType) {
      @Override
      protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
        ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
        if (exchange == null) return Collections.emptySet();
        var conformanceMessage = this.httpMessageType == HttpMessageType.RESPONSE
          ? exchange.getResponse().message()
          : exchange.getRequest().message();
        var responsePayload = conformanceMessage.body().getJsonBody();
        if (responsePayload instanceof ObjectNode booking) {
          return subCheck.apply(booking);
        }
        return Set.of("Could not perform the check as the payload was not correct format");
      }
    };
  }

  protected boolean isNonEmptyNode(JsonNode field) {
    if (field == null || field.isMissingNode()) {
      return false;
    }
    if (field.isTextual()) {
      return !field.asText().isBlank();
    }
    return !field.isEmpty() || field.isValueNode();
  }
}
