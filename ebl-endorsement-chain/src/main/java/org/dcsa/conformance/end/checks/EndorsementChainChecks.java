package org.dcsa.conformance.end.checks;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.ConformanceError;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.end.party.EndorsementChainRole;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.dcsa.conformance.end.checks.EndChainDataSets.VALID_ACTION_CODES;
import static org.dcsa.conformance.end.checks.EndChainDataSets.VALID_CODE_LIST_PROVIDERS;
import static org.dcsa.conformance.end.checks.EndChainDataSets.VALID_EBL_PLATFORMS;

public class EndorsementChainChecks {

  private static final String IDENTIFYING_CODES = "identifyingCodes";
  private static final String CODE_LIST_PROVIDER = "codeListProvider";
  private static final String REPRESENTED_PARTY = "representedParty";
  private static final String ENDORSEMENT_CHAIN = "endorsementChain";
  private static final String ACTOR = "actor";
  private static final String RECIPIENT = "recipient";
  private static final String EBL_PLATFORM = "eblPlatform";
  private static final String ACTION_CODE = "actionCode";

  public static ActionCheck getENDGetResponseChecks(
    UUID matchedExchangeUuid, String expectedApiVersion) {

    var checks = new ArrayList<JsonContentCheck>();
    checks.add(validResponseIsNonEmptyArray());
    checks.add(validActionCode());
    checks.add(validEblPlatformPseudoEnum());
    checks.add(validCodeListProviderPseudoEnumEverywhere());
    return JsonAttribute.contentChecks(
      "",
      "Validate the payload from the Provider",
      EndorsementChainRole::isProvider,
      matchedExchangeUuid,
      HttpMessageType.RESPONSE,
      expectedApiVersion,
      checks);
  }

  private static JsonContentCheck validResponseIsNonEmptyArray() {
    return JsonAttribute.customValidator(
      "Response must be a non-empty array",
      body -> {
        Set<String> errors = new LinkedHashSet<>();

        if (body.isEmpty() || !body.isArray()) {
          errors.add("Response must be a non-empty array");
        }

        return ConformanceCheckResult.simple(errors);
      });
  }

  private static JsonContentCheck validActionCode() {
    return JsonAttribute.customValidator(
      "All 'endorsementChain.actionCode' values must be one of the values defined by the END standard",
      body -> {
        Set<String> errors = new LinkedHashSet<>();

        if (body.isEmpty() || !body.isArray()) {
          return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
        }

        for (int docIdx = 0; docIdx < body.size(); docIdx++) {
          JsonNode endorsementChain = body.get(docIdx).path(ENDORSEMENT_CHAIN);
          if (!endorsementChain.isArray() || endorsementChain.isEmpty()) {
            continue;
          }

          for (int entryIdx = 0; entryIdx < endorsementChain.size(); entryIdx++) {
            JsonNode actionCodeNode = endorsementChain.get(entryIdx).path(ACTION_CODE);

            if (actionCodeNode.isMissingNode() || actionCodeNode.isNull()) {
              continue;
            }

            String actionCode = actionCodeNode.asText();
            if (actionCode.isBlank()) {
              continue;
            }

            if (!VALID_ACTION_CODES.contains(actionCode)) {
              errors.add(
                "[%d].endorsementChain[%d].actionCode: '%s' is not a valid action code. Must be one of: %s"
                  .formatted(docIdx, entryIdx, actionCode, VALID_ACTION_CODES));
            }
          }
        }
        return ConformanceCheckResult.simple(errors);
      });
  }

  private static JsonContentCheck validEblPlatformPseudoEnum() {
    return JsonAttribute.customValidator(
      "All 'eblPlatform' values must be from the DCSA eBL solution providers code list",
      body -> {
        if (body.isEmpty() || !body.isArray()) {
          return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
        }
        Set<String> errors = new LinkedHashSet<>();

        for (int docIdx = 0; docIdx < body.size(); docIdx++) {
          JsonNode endorsementChain = body.get(docIdx).path(ENDORSEMENT_CHAIN);
          if (!endorsementChain.isArray()) {
            continue;
          }

          for (int entryIdx = 0; entryIdx < endorsementChain.size(); entryIdx++) {
            JsonNode entry = endorsementChain.get(entryIdx);

            validatePseudoEnumIfPresent(
              errors,
              entry.path(ACTOR).path(EBL_PLATFORM),
              "[%d].endorsementChain[%d].actor.eblPlatform".formatted(docIdx, entryIdx),
              VALID_EBL_PLATFORMS);

            validatePseudoEnumIfPresent(
              errors,
              entry.path(RECIPIENT).path(EBL_PLATFORM),
              "[%d].endorsementChain[%d].recipient.eblPlatform".formatted(docIdx, entryIdx),
              VALID_EBL_PLATFORMS);
          }
        }

        return ConformanceCheckResult.simple(errors);
      });
  }

  private static JsonContentCheck validCodeListProviderPseudoEnumEverywhere() {
    return JsonAttribute.customValidator(
      "All 'codeListProvider' values must be from the DCSA party code list providers code list",
      body -> {
        if (body.isEmpty() || !body.isArray()) {
          return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
        }
        Set<String> errors = new LinkedHashSet<>();

        for (int docIdx = 0; docIdx < body.size(); docIdx++) {
          JsonNode endorsementChain = body.get(docIdx).path(ENDORSEMENT_CHAIN);
          if (!endorsementChain.isArray()) {
            continue;
          }

          for (int entryIdx = 0; entryIdx < endorsementChain.size(); entryIdx++) {
            JsonNode entry = endorsementChain.get(entryIdx);

            validateCodeListProviderForPartyAndRepresentedParty(
              errors,
              entry.path(ACTOR),
              "[%d].endorsementChain[%d].actor".formatted(docIdx, entryIdx));

            validateCodeListProviderForPartyAndRepresentedParty(
              errors,
              entry.path(RECIPIENT),
              "[%d].endorsementChain[%d].recipient".formatted(docIdx, entryIdx));
          }
        }

        return ConformanceCheckResult.simple(errors);
      });
  }

  private static void validateCodeListProviderForPartyAndRepresentedParty(
    Set<String> errors, JsonNode partyNode, String partyPath) {

    if (partyNode.isMissingNode() || partyNode.isNull()) {
      return;
    }
    validateCodeListProvidersUnderParty(errors, partyNode, partyPath);

    JsonNode representedParty = partyNode.path(REPRESENTED_PARTY);
    if (!representedParty.isMissingNode() && !representedParty.isNull()) {
      validateCodeListProvidersUnderParty(
        errors, representedParty, partyPath + "." + REPRESENTED_PARTY);
    }
  }

  private static void validateCodeListProvidersUnderParty(
    Set<String> errors, JsonNode partyNode, String partyPath) {

    JsonNode identifyingCodes = partyNode.path(IDENTIFYING_CODES);
    if (!identifyingCodes.isArray()) {
      return;
    }

    for (int idIdx = 0; idIdx < identifyingCodes.size(); idIdx++) {
      JsonNode clpNode = identifyingCodes.get(idIdx).path(CODE_LIST_PROVIDER);

      validatePseudoEnumIfPresent(
        errors,
        clpNode,
        "%s.identifyingCodes[%d].codeListProvider".formatted(partyPath, idIdx),
        VALID_CODE_LIST_PROVIDERS);
    }
  }

  private static void validatePseudoEnumIfPresent(
    Set<String> errors, JsonNode node, String jsonPathForError, Set<String> allowedValues) {

    if (node.isMissingNode() || node.isNull()) {
      return;
    }

    String value = node.asText();
    if (value.isBlank()) {
      errors.add("%s: must be a non-empty string".formatted(jsonPathForError));
      return;
    }

    if (!allowedValues.contains(value)) {
      errors.add(
        "%s: '%s' is not allowed. Must be one of: %s"
          .formatted(jsonPathForError, value, allowedValues));
    }
  }
}
