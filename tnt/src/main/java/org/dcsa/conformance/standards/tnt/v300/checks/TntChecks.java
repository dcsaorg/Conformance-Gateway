package org.dcsa.conformance.standards.tnt.v300.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.tnt.v300.party.TntRole;

@UtilityClass
public class TntChecks {

  public static ActionCheck getTntGetResponseChecks(UUID matched, String standardVersion) {
    List<JsonContentCheck> checks = new ArrayList<>();

    //checks.add(atLeastOneVgmDeclarationInMessageCheck());

    return JsonAttribute.contentChecks(
            TntRole::isProducer, matched, HttpMessageType.RESPONSE, standardVersion, checks);
  }

  public static ActionCheck getTntPostPayloadChecks(UUID matched, String standardVersion) {
    List<JsonContentCheck> checks = new ArrayList<>();

    //checks.add(atLeastOneVgmDeclarationInMessageCheck());

    return JsonAttribute.contentChecks(
            TntRole::isProducer, matched, HttpMessageType.REQUEST, standardVersion, checks);
  }
}
