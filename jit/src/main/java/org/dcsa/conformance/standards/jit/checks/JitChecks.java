package org.dcsa.conformance.standards.jit.checks;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.NoArgsConstructor;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.jit.model.PortCallServiceType;
import org.dcsa.conformance.standards.jit.party.DynamicScenarioParameters;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class JitChecks {

  public static ActionCheck createChecksForPortCallService(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      PortCallServiceType serviceType) {
    return JsonAttribute.contentChecks(
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        List.of(checkPortCallService(serviceType)));
  }

  private static JsonContentCheck checkPortCallService(PortCallServiceType serviceType) {
    return JsonAttribute.customValidator(
        "Check if the correct Port Call Service ('%s') was supplied.".formatted(serviceType.name()),
        body -> {
          if (body.has("specification")) {
            var actualServiceType = body.get("specification").get("portCallServiceType").asText();
            if (!serviceType.name().equals(actualServiceType)) {
              return Set.of(
                  "Expected Port Call Service type '%s' but got '%s'"
                      .formatted(serviceType.name(), actualServiceType));
            }
          }
          return Collections.emptySet();
        });
  }

  public static ActionCheck createChecksForTimestamp(
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    String expectedApiVersion,
    DynamicScenarioParameters dsp) {
    return JsonAttribute.contentChecks(
      isRelevantForRoleName,
      matchedExchangeUuid,
      HttpMessageType.REQUEST,
      expectedApiVersion,
      List.of(checkPlannedMatchesRequestedTimestamp(dsp)));
  }

  private static JsonContentCheck checkPlannedMatchesRequestedTimestamp(DynamicScenarioParameters dsp) {
    // TODO: Implement this check
    return null;
  }
}
