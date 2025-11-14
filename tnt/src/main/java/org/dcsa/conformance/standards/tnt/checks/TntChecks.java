package org.dcsa.conformance.standards.tnt.checks;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.tnt.party.TntRole;

@UtilityClass
public class TntChecks {

  public static ActionCheck responseContentChecks(
      UUID matched, String standardVersion) {
    var checks = new ArrayList<JsonContentCheck>();

    checks.add(VALIDATE_NON_EMPTY_EVENTS);

    return JsonAttribute.contentChecks(
        TntRole::isPublisher, matched, HttpMessageType.RESPONSE, standardVersion, checks);
  }

  public JsonContentCheck VALIDATE_NON_EMPTY_EVENTS =
      JsonAttribute.customValidator(
          "Every response received during a conformance test must contain events",
          body ->
              ConformanceCheckResult.simple(
                  TntSchemaConformanceCheck.findEventNodes(body).isEmpty()
                      ? Set.of("No events found in response")
                      : Set.of()));
}
