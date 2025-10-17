package org.dcsa.conformance.standards.ebl.checks;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public abstract class PayloadContentConformanceCheck extends ActionCheck {

  public static final String VALIDATE_PAYLOAD_PREFIX = "[Notification]";
  public static final String VALIDATE_PAYLOAD_TITLE = "Validate the carrier payload";

  protected PayloadContentConformanceCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType) {
    super(
        VALIDATE_PAYLOAD_PREFIX,
        VALIDATE_PAYLOAD_TITLE,
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType);
  }

  @Override
  protected final ConformanceCheckResult performCheck(
      Function<UUID, ConformanceExchange> getExchangeByUuid) {
    // All checks are delegated to sub-checks; nothing to do in here.
    return ConformanceCheckResult.simple(Collections.emptySet());
  }

  @Override
  protected abstract Stream<? extends ConformanceCheck> createSubChecks();

  protected ConformanceCheck createSubCheck(
      String prefix, String subtitle, Function<JsonNode, Set<String>> subCheck) {
    return new ActionCheck(
        prefix, subtitle, this::isRelevantForRole, this.matchedExchangeUuid, this.httpMessageType) {
      @Override
      protected ConformanceCheckResult performCheck(
          Function<UUID, ConformanceExchange> getExchangeByUuid) {
        ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
        if (exchange == null) return ConformanceCheckResult.simple(Collections.emptySet());
        var conformanceMessage =
            this.httpMessageType == HttpMessageType.RESPONSE
                ? exchange.getResponse().message()
                : exchange.getRequest().message();
        var payload = conformanceMessage.body().getJsonBody();
        return ConformanceCheckResult.simple(subCheck.apply(payload));
      }
    };
  }

  protected Function<JsonNode, Set<String>> at(
      String path, Function<JsonNode, Set<String>> subCheck) {
    var pointer = JsonPointer.compile(path);
    return payload -> subCheck.apply(payload.at(pointer));
  }

  protected Stream<ConformanceCheck> buildChecks(
      String label, String jsonPath, Supplier<List<JsonContentCheck>> checksSupplier) {
    return checksSupplier.get().stream().map(check -> wrapWithSubCheck(label, jsonPath, check));
  }

  protected ConformanceCheck wrapWithSubCheck(String label, String path, JsonContentCheck check) {
    return createSubCheck(
        label,
        check.description(),
        at(
            path,
            jsonNode -> {
              if (jsonNode.isMissingNode() || jsonNode.isEmpty()) {
                return Set.of();
              }
              return check.validate(jsonNode);
            }));
  }
}
