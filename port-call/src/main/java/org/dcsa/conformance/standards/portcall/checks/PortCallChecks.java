package org.dcsa.conformance.standards.portcall.checks;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.portcall.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.portcall.party.PortCallRole;

public class PortCallChecks {
  public static ActionCheck getPortCallPostPayloadChecks(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<DynamicScenarioParameters> dsp) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE);
    return JsonAttribute.contentChecks(
      "",
      "The Publisher has correctly demonstrated the use of functionally required attributes in the payload",
      PortCallRole::isPublisher,
      matchedExchangeUuid,
      HttpMessageType.REQUEST,
      expectedApiVersion,
      checks);
  }

  private static final JsonContentCheck VALIDATE_NON_EMPTY_RESPONSE =
      JsonAttribute.customValidator(
          "Every response received during a conformance test must not be empty",
          body -> {
            var eventsNode = body.get("events");

            if (eventsNode == null || !eventsNode.isArray() || eventsNode.isEmpty()) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "At least one event must be included in a message sent to the sandbox during conformance testing"));
            }

            boolean hasNonEmptyEvent =
                StreamSupport.stream(eventsNode.spliterator(), false)
                    .filter(JsonNode::isObject)
                    .anyMatch(
                        eventObj ->
                            eventObj.fieldNames().hasNext()
                                && StreamSupport.stream(
                                        Spliterators.spliteratorUnknownSize(
                                            eventObj.fieldNames(), 0),
                                        false)
                                    .map(eventObj::get)
                                    .anyMatch(
                                        field ->
                                            (field.isValueNode() && !field.asText().isBlank())
                                                || (field.isContainerNode() && !field.isEmpty())));

            if (!hasNonEmptyEvent) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "At least one event must be included in a message sent to the sandbox during conformance testing"));
            }
            return ConformanceCheckResult.simple(Set.of());
          });

  public static ActionCheck getGetResponsePayloadChecks(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<DynamicScenarioParameters> dsp) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE);
    return JsonAttribute.contentChecks(
      "",
      "The Publisher has correctly demonstrated the use of functionally required attributes in the payload",
      PortCallRole::isPublisher,
      matchedExchangeUuid,
      HttpMessageType.RESPONSE,
      expectedApiVersion,
      checks);
  }

  private static List<JsonContentCheck> payloadChecks(String scenarioType) {
    var checks = new ArrayList<JsonContentCheck>();

    return checks;
  }
}
