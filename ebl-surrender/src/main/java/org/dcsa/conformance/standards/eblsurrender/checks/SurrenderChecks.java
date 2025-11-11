package org.dcsa.conformance.standards.eblsurrender.checks;

import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES;

import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonRebasableContentCheck;
import org.dcsa.conformance.core.check.KeywordDataset;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;

@UtilityClass
public class SurrenderChecks {

  private static final KeywordDataset SURRENDER_ACTIONS_DATA_SET =
      KeywordDataset.staticDataset(
          "ISSUE", "ENDORSE", "SIGN", "SURRENDER_FOR_DELIVERY", "SURRENDER_FOR_AMENDMENT");

  private static final String ENDORSEMENT_CHAIN = "endorsementChain";
  private static final String ACTION_CODE = "actionCode";
  private static final String CODE_LIST_PROVIDER = "codeListProvider";
  private static final String ACTOR = "actor";
  private static final String IDENTIFYING_CODES = "identifyingCodes";
  private static final String RECIPIENT = "recipient";

  private static final String S_x_S = "%s.*.%s";
  private static final String S_x_S_S_x_S = "%s.*.%s.%s.*.%s";

  private static final JsonContentCheck SURRENDER_ACTION_VALIDATION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate Surrender Actions",
          mav -> mav.submitAllMatching(S_x_S.formatted(ENDORSEMENT_CHAIN, ACTION_CODE)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(SURRENDER_ACTIONS_DATA_SET));

  private static final JsonRebasableContentCheck SURRENDER_PARTY_CODE_LIST_PROVIDER =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate '%s' is a known value".formatted(CODE_LIST_PROVIDER),
          mav -> {
            mav.submitAllMatching(
                S_x_S_S_x_S.formatted(
                    ENDORSEMENT_CHAIN, ACTOR, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_x_S_S_x_S.formatted(
                    ENDORSEMENT_CHAIN, RECIPIENT, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
          },
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
              DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES));

  public static ActionCheck surrenderRequestChecks(UUID matched, String standardVersion) {
    return JsonAttribute.contentChecks(
        EblSurrenderRole::isPlatform,
        matched,
        HttpMessageType.REQUEST,
        standardVersion,
        SURRENDER_ACTION_VALIDATION,
        SURRENDER_PARTY_CODE_LIST_PROVIDER);
  }
}
