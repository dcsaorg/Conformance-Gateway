package org.dcsa.conformance.standards.an.checks;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.an.party.ANRole;
import org.dcsa.conformance.standards.an.party.DynamicScenarioParameters;

public class ANChecks {

  public static ActionCheck getANPayloadChecks(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<DynamicScenarioParameters> dspSupplier) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE);
    checks.add(CHECK_PRESENCE_OF_REQUIRED_FIELDS);
    return JsonAttribute.contentChecks(
        ANRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        checks);
  }

  private static final JsonContentCheck CHECK_PRESENCE_OF_REQUIRED_FIELDS =
      JsonAttribute.customValidator(
          "The Publisher has correctly demonstrated the use of functionally required attributes in the payload",
          body -> {
            var issues = new LinkedHashSet<String>();
            var arrivalNotices = body.get("arrivalNotices");
            for (int i = 0; i < arrivalNotices.size(); i++) {
              var item = arrivalNotices.get(i);
              if (!item.hasNonNull("carrierCode")) {
                issues.add(String.format("Item %d: carrierCode must be present in the payload", i));
              }
              if (!item.hasNonNull("carrierCodeListProvider")) {
                issues.add(
                    String.format(
                        "Item %d: carrierCodeListProvider must be present in the payload", i));
              }
            }

            return issues;
          });

  static final JsonContentCheck VALIDATE_NON_EMPTY_RESPONSE =
      JsonAttribute.customValidator(
          "Every response received during a conformance test must not be empty",
          body -> body.isEmpty() ? Set.of("The response body must not be empty") : Set.of());
}
