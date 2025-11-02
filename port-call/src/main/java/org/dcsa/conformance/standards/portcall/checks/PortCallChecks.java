package org.dcsa.conformance.standards.portcall.checks;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.portcall.party.PortCallRole;

public class PortCallChecks {
  public static ActionCheck getPortCallPostPayloadChecks(
    UUID matchedExchangeUuid, String expectedApiVersion) {
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
      body -> ConformanceCheckResult.simple(body.isEmpty() ? Set.of("The response body must not be empty") : Set.of()));

  public static ActionCheck getGetResponsePayloadChecks(
    UUID matchedExchangeUuid, String expectedApiVersion) {
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
}
