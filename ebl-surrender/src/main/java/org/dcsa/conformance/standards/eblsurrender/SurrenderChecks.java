package org.dcsa.conformance.standards.eblsurrender;

import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonRebaseableContentCheck;
import org.dcsa.conformance.core.check.KeywordDataset;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;

import java.util.UUID;

import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES;

public class SurrenderChecks {
  private static final KeywordDataset SURRENDER_ACTIONS_DATA_SET = KeywordDataset.staticDataset(
    "ISSUE",
    "ENDORSE",
    "SIGN",
    "SURRENDER_FOR_DELIVERY",
    "SURRENDER_FOR_AMENDMENT"
  );

  private static final JsonContentCheck SURRENDER_ACTION_VALIDATION = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate Surrender Actions",
    (mav) -> mav.submitAllMatching("endorsementChain.*.actionCode"),
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(SURRENDER_ACTIONS_DATA_SET)
  );

  private static final JsonRebaseableContentCheck SURRENDER_PARTY_CODE_LIST_PROVIDER = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate 'codeListProvider' is a known value",
    (mav) -> {
      mav.submitAllMatching("endorsementChain.*.actor.identifyingCodes.*.codeListProvider");
      mav.submitAllMatching("endorsementChain.*.recipient.identifyingCodes.*.codeListProvider");
    },
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES)
  );

  public static ActionCheck surrenderRequestChecks(UUID matched, String standardVersion) {
    return JsonAttribute.contentChecks(
      EblSurrenderRole::isPlatform,
      matched,
      HttpMessageType.REQUEST,
      standardVersion,
      SURRENDER_ACTION_VALIDATION,
      SURRENDER_PARTY_CODE_LIST_PROVIDER
    );
  }
}
