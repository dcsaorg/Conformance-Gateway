package org.dcsa.conformance.standards.eblsurrender.checks;

import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.KeywordDataset;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;

import java.util.List;
import java.util.UUID;

import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.EBL_PLATFORMS_DATASET;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.REASON_CODES;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.SURRENDER_ACTIONS_DATA_SET;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.SURRENDER_DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES;

@UtilityClass
public class SurrenderChecks {

  private static final String ENDORSEMENT_CHAIN = "endorsementChain";
  private static final String ACTION_CODE = "actionCode";
  private static final String ACTION = "action";
  private static final String SURRENDER_REQUEST_CODE = "surrenderRequestCode";
  private static final String CODE_LIST_PROVIDER = "codeListProvider";
  private static final String EBL_PLATFORM = "eblPlatform";
  private static final String REASON_CODE = "reasonCode";
  private static final String ACTOR = "actor";
  private static final String IDENTIFYING_CODES = "identifyingCodes";
  private static final String RECIPIENT = "recipient";
  private static final String REPRESENTED_PARTY = "representedParty";
  private static final String ENDORSEMENT_CHAIN_ACTION_CODE = "endorsementChain.actionCode";

  private static final String REQUEST_CODE_DESCRIPTION =
    "The surrender request has the appropriate '%s' for the scenario being tested"
      .formatted(SURRENDER_REQUEST_CODE);
  private static final String ACTION_CODE_DESCRIPTION =
    "All '%s' values are one of: 'ISSUE', 'ENDORSE', 'SIGN', 'SURRENDER_FOR_DELIVERY', 'SURRENDER_FOR_AMENDMENT', 'BLANK_ENDORSE', 'ENDORSE_TO_ORDER', 'TRANSFER', 'SURRENDERED'"
      .formatted(ENDORSEMENT_CHAIN_ACTION_CODE);
  private static final String CODE_LIST_PROVIDER_DESCRIPTION =
    "All '%s' values are from the latest DCSA party code list providers"
      .formatted(CODE_LIST_PROVIDER);
  private static final String EBL_PLATFORM_DESCRIPTION =
    "All '%s' values are from the latest DCSA eBL solution providers".formatted(EBL_PLATFORM);
  private static final String REASON_CODE_DESCRIPTION =
    "The '%s' (if present) is one of: 'SWTP', 'COD', 'SWI'".formatted(REASON_CODE);
  private static final String RESPONSE_ACTION_DESCRIPTION =
    "The surrender response has a valid '%s' code ('SURR' or 'SREJ')".formatted(ACTION);

  private static final String TWO_LEVEL_PATH = "%s.*.%s";
  private static final String FOUR_LEVEL_PATH = "%s.*.%s.%s.*.%s";
  private static final String FIVE_LEVEL_PATH = "%s.*.%s.%s.%s.*.%s";
  private static final String THREE_LEVEL_PATH = "%s.*.%s.%s";

  private static final KeywordDataset SURRENDER_RESPONSE_ACTIONS =
    KeywordDataset.staticDataset("SURR", "SREJ");

  private static final JsonContentCheck SURRENDER_ACTION_VALIDATION =
    JsonAttribute.allIndividualMatchesMustBeValid(
      ACTION_CODE_DESCRIPTION,
      mav -> mav.submitAllMatching(TWO_LEVEL_PATH.formatted(ENDORSEMENT_CHAIN, ACTION_CODE)),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(SURRENDER_ACTIONS_DATA_SET));

  private static final JsonContentCheck SURRENDER_PARTY_CODE_LIST_PROVIDER =
    JsonAttribute.allIndividualMatchesMustBeValid(
      CODE_LIST_PROVIDER_DESCRIPTION,
      mav -> {
        mav.submitAllMatching(
          FOUR_LEVEL_PATH.formatted(
            ENDORSEMENT_CHAIN, ACTOR, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
        mav.submitAllMatching(
          FOUR_LEVEL_PATH.formatted(
            ENDORSEMENT_CHAIN, RECIPIENT, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
        mav.submitAllMatching(
          FIVE_LEVEL_PATH
            .formatted(
              ENDORSEMENT_CHAIN,
              ACTOR,
              REPRESENTED_PARTY,
              IDENTIFYING_CODES,
              CODE_LIST_PROVIDER));
        mav.submitAllMatching(
          FIVE_LEVEL_PATH
            .formatted(
              ENDORSEMENT_CHAIN,
              RECIPIENT,
              REPRESENTED_PARTY,
              IDENTIFYING_CODES,
              CODE_LIST_PROVIDER));
      },
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
        SURRENDER_DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES));

  private static final JsonContentCheck EBL_PLATFORM_CHECK =
    JsonAttribute.allIndividualMatchesMustBeValid(
      EBL_PLATFORM_DESCRIPTION,
      mav -> {
        mav.submitAllMatching(
          THREE_LEVEL_PATH.formatted(ENDORSEMENT_CHAIN, ACTOR, EBL_PLATFORM));
        mav.submitAllMatching(
          THREE_LEVEL_PATH.formatted(ENDORSEMENT_CHAIN, RECIPIENT, EBL_PLATFORM));
      },
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EBL_PLATFORMS_DATASET));

  private static final JsonContentCheck REASON_CODE_CHECK =
    JsonAttribute.allIndividualMatchesMustBeValid(
      REASON_CODE_DESCRIPTION,
      mav -> mav.submitAllMatching(REASON_CODE),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(REASON_CODES));

  private static final JsonContentCheck RESPONSE_ACTION_CHECK =
    JsonAttribute.allIndividualMatchesMustBeValid(
      RESPONSE_ACTION_DESCRIPTION,
      mav -> mav.submitAllMatching(ACTION),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(SURRENDER_RESPONSE_ACTIONS));

  public static List<JsonContentCheck> surrenderRequestContentChecks(
    String expectedSurrenderRequestCode) {
    JsonContentCheck requestCodeCheck =
      JsonAttribute.allIndividualMatchesMustBeValid(
        REQUEST_CODE_DESCRIPTION,
        mav -> mav.submitAllMatching(SURRENDER_REQUEST_CODE),
        JsonAttribute.matchedMustEqual(() -> expectedSurrenderRequestCode));
    return List.of(
      requestCodeCheck,
      SURRENDER_ACTION_VALIDATION,
      SURRENDER_PARTY_CODE_LIST_PROVIDER,
      EBL_PLATFORM_CHECK,
      REASON_CODE_CHECK);
  }

  public static List<JsonContentCheck> surrenderResponseContentChecks() {
    return List.of(RESPONSE_ACTION_CHECK);
  }

  public static ActionCheck surrenderRequestChecks(
    UUID matched, String standardVersion, String expectedSurrenderRequestCode) {
    return JsonAttribute.contentChecks(
      EblSurrenderRole::isPlatform,
      matched,
      HttpMessageType.REQUEST,
      standardVersion,
      surrenderRequestContentChecks(expectedSurrenderRequestCode));
  }

  public static ActionCheck surrenderResponseChecks(UUID matched, String standardVersion) {
    return JsonAttribute.contentChecks(
      "[Response]",
      EblSurrenderRole::isCarrier,
      matched,
      HttpMessageType.REQUEST,
      standardVersion,
      surrenderResponseContentChecks());
  }
}
