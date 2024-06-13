package org.dcsa.conformance.standards.eblsurrender;

import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.KeywordDataset;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;

import java.util.UUID;

public class SurrenderChecks {
  private static final KeywordDataset SURRENDER_ACTIONS_DATA_SET = KeywordDataset.staticDataset(
    "ISSUE",
    "ENDORSE",
    "SIGN",
    "SURRENDER FOR DELIVERY",
    "SURRENDER FOR AMENDMENT"
  );

  private static final JsonContentCheck SURRENDER_ACTION_VALIDATION = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate Surrender Actions",
    (mav) -> mav.submitAllMatching("endorsementChain.*.actionCode"),
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(SURRENDER_ACTIONS_DATA_SET)
  );

  public static ActionCheck surrenderRequestChecks(UUID matched, String standardVersion) {
    return JsonAttribute.contentChecks(
      EblSurrenderRole::isPlatform,
      matched,
      HttpMessageType.REQUEST,
      standardVersion,
      SURRENDER_ACTION_VALIDATION
    );

  }
}
