package org.dcsa.conformance.standards.tnt.v300.checks;

import java.util.ArrayList;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.tnt.v300.party.TntRole;

@UtilityClass
public class TntChecks {

  public static ActionCheck getTntPostPayloadChecks(
      UUID matchedExchangeUuid, String expectedApiVersion, String scenarioType) {

    var checks = new ArrayList<JsonContentCheck>();

    return JsonAttribute.contentChecks(
        "",
        "The Producer has correctly demonstrated the use of functionally required attributes in the payload",
        TntRole::isProducer,
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        checks);
  }
}
