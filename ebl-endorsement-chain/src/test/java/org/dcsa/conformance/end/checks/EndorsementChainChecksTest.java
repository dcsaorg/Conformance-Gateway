package org.dcsa.conformance.end.checks;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndorsementChainChecksTest {

  @Test
  void publicFactoryBuildsAllPayloadChecks() {
    new EndorsementChainChecks();

    ActionCheck check = EndorsementChainChecks.getENDGetResponseChecks(UUID.randomUUID(), "3.0.3");

    assertEquals(4, check.subChecksStream().count());
  }

  @Test
  void responseMustBeANonEmptyArray() {
    JsonContentCheck check = privateCheck("validResponseIsNonEmptyArray");

    assertInvalidWithMessage(check, OBJECT_MAPPER.createArrayNode(), "non-empty array");
    assertInvalidWithMessage(check, json("{\"value\": 1}"), "non-empty array");
    assertTrue(check.validate(json("[{}]")).isConformant());
  }

  @Test
  void actionCodeValidationCoversIrrelevantStructuralAndValueBranches() {
    JsonContentCheck check = privateCheck("validActionCode");

    assertIrrelevant(check, OBJECT_MAPPER.createArrayNode());
    assertIrrelevant(check, json("{\"value\": 1}"));
    assertTrue(check.validate(validPayload()).isConformant());
    assertInvalidWithMessage(check, branchPayload(), "UNKNOWN");
  }

  @Test
  void eblPlatformValidationCoversIrrelevantStructuralAndValueBranches() {
    JsonContentCheck check = privateCheck("validEblPlatformPseudoEnum");

    assertIrrelevant(check, OBJECT_MAPPER.createArrayNode());
    assertIrrelevant(check, json("{\"value\": 1}"));
    assertTrue(check.validate(validPayload()).isConformant());
    ConformanceCheckResult result = check.validate(branchPayload());
    assertFalse(result.isConformant());
    assertTrue(result.getErrorMessages().stream().anyMatch(message -> message.contains("non-empty string")));
    assertTrue(result.getErrorMessages().stream().anyMatch(message -> message.contains("NOPE")));
  }

  @Test
  void codeListProviderValidationCoversEveryNestedPartyBranch() {
    JsonContentCheck check = privateCheck("validCodeListProviderPseudoEnumEverywhere");

    assertIrrelevant(check, OBJECT_MAPPER.createArrayNode());
    assertIrrelevant(check, json("{\"value\": 1}"));
    assertTrue(check.validate(validPayload()).isConformant());
    ConformanceCheckResult result = check.validate(branchPayload());
    assertFalse(result.isConformant());
    assertTrue(result.getErrorMessages().stream().anyMatch(message -> message.contains("non-empty string")));
    assertTrue(result.getErrorMessages().stream().anyMatch(message -> message.contains("UNKNOWN")));
  }

  private static JsonNode validPayload() {
    return json(
      """
        [
          {
            "endorsementChain": [
              {
                "actionCode": "ISSUE",
                "actor": {
                  "eblPlatform": "DOCU",
                  "identifyingCodes": [{"codeListProvider": "AEOT", "partyCode": "A"}],
                  "representedParty": {
                    "identifyingCodes": [{"codeListProvider": "SGTD", "partyCode": "B"}]
                  }
                },
                "recipient": {
                  "eblPlatform": "WAVE",
                  "identifyingCodes": [{"codeListProvider": "DCSA", "partyCode": "C"}]
                }
              }
            ]
          }
        ]
        """);
  }

  private static JsonNode branchPayload() {
    return json(
      """
        [
          {},
          {"endorsementChain": {}},
          {"endorsementChain": []},
          {"endorsementChain": [
            {},
            {"actionCode": null, "actor": null, "recipient": {"identifyingCodes": {}}},
            {
              "actionCode": " ",
              "actor": {
                "eblPlatform": null,
                "identifyingCodes": [],
                "representedParty": null
              },
              "recipient": {
                "eblPlatform": "",
                "identifyingCodes": [{}]
              }
            },
            {
              "actionCode": "ISSUE",
              "actor": {
                "eblPlatform": "WAVE",
                "identifyingCodes": [{"codeListProvider": null}],
                "representedParty": {
                  "identifyingCodes": [{"codeListProvider": "DCSA"}]
                }
              },
              "recipient": {
                "eblPlatform": "NOPE",
                "identifyingCodes": [{"codeListProvider": ""}]
              }
            },
            {
              "actionCode": "UNKNOWN",
              "actor": {"identifyingCodes": [{"codeListProvider": "UNKNOWN"}]},
              "recipient": null
            }
          ]}
        ]
        """);
  }

  private static void assertIrrelevant(JsonContentCheck check, JsonNode payload) {
    ConformanceCheckResult result = check.validate(payload);
    assertTrue(result.isConformant());
    assertFalse(result.isRelevant());
  }

  private static void assertInvalidWithMessage(
    JsonContentCheck check, JsonNode payload, String expectedMessagePart) {
    ConformanceCheckResult result = check.validate(payload);
    assertFalse(result.isConformant());
    assertTrue(
      result.getErrorMessages().stream().anyMatch(message -> message.contains(expectedMessagePart)),
      () -> "Expected an error containing '%s', but got %s"
        .formatted(expectedMessagePart, result.getErrorMessages()));
  }

  private static JsonNode json(String value) {
    try {
      return OBJECT_MAPPER.readTree(value);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to create payload for test", e);
    }
  }

  private static JsonContentCheck privateCheck(String methodName) {
    try {
      Method method = EndorsementChainChecks.class.getDeclaredMethod(methodName);
      method.setAccessible(true);
      return (JsonContentCheck) method.invoke(null);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to access check method: " + methodName, e);
    }
  }
}
