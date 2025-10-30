package org.dcsa.conformance.standards.portcall.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.NoArgsConstructor;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.portcall.model.PortCallServiceTypeCode;
import org.dcsa.conformance.standards.portcall.party.DynamicScenarioParameters;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class JitChecks {



  public static ActionCheck createChecksForPortCallService(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      PortCallServiceTypeCode serviceType,
      DynamicScenarioParameters dsp) {
    if (dsp == null) return null;
    List<JsonContentCheck> checks = new ArrayList<>();


    return JsonAttribute.contentChecks(
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        checks);
  }


}
