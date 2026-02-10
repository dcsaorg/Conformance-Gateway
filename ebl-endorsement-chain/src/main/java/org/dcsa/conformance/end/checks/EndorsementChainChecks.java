package org.dcsa.conformance.end.checks;

import java.util.ArrayList;
import java.util.UUID;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.end.party.EndorsementChainRole;

public class EndorsementChainChecks {

  public static ActionCheck getENDGetResponseChecks(
      UUID matchedExchangeUuid, String expectedApiVersion, String scenarioType) {

    var checks = new ArrayList<JsonContentCheck>();

    return JsonAttribute.contentChecks(
        "",
        "The Provider has correctly demonstrated the use of functionally required attributes in the payload",
        EndorsementChainRole::isProvider,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE,
        expectedApiVersion,
        checks);
  }
}
