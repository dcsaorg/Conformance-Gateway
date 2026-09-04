package org.dcsa.conformance.standards.eblsurrender.checks;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurrenderChecksTest {

  private static final List<String> REQUEST_CHECK_DESCRIPTIONS =
    List.of(
      "The surrender request has the appropriate 'surrenderRequestCode' for the scenario being tested",
      "All 'endorsementChain.actionCode' values are one of: 'ISSUE', 'ENDORSE', 'SIGN', 'SURRENDER_FOR_DELIVERY', 'SURRENDER_FOR_AMENDMENT', 'BLANK_ENDORSE', 'ENDORSE_TO_ORDER', 'TRANSFER', 'SURRENDERED'",
      "All 'codeListProvider' values are from the latest DCSA party code list providers",
      "All 'eblPlatform' values are from the latest DCSA eBL solution providers",
      "The 'reasonCode' (if present) is one of: 'SWTP', 'COD', 'SWI'");

  @Test
  void requestChecksUseDocumentedDescriptions() {
    assertEquals(
      REQUEST_CHECK_DESCRIPTIONS,
      requestChecks("SREQ").stream().map(JsonContentCheck::description).toList());
  }

  @Test
  void responseCheckUsesDocumentedDescription() {
    assertEquals(
      List.of("The surrender response has a valid 'action' code ('SURR' or 'SREJ')"),
      SurrenderChecks.surrenderResponseContentChecks().stream()
        .map(JsonContentCheck::description)
        .toList());
  }

  @ParameterizedTest
  @ValueSource(strings = {"SREQ", "AREQ"})
  void validRequestPassesEveryCheck(String requestCode) {
    ObjectNode request = validRequest(requestCode);

    requestChecks(requestCode).forEach(check -> assertConformant(check, request));
  }

  @Test
  void missingOptionalReasonCodeIsAccepted() {
    ObjectNode request = validRequest("SREQ");
    request.remove("reasonCode");

    assertConformant(requestChecks("SREQ").get(4), request);
  }

  @Test
  void requestCodeMustMatchTheScenario() {
    assertNonConformant(requestChecks("AREQ").get(0), validRequest("SREQ"));
  }

  @ParameterizedTest
  @ValueSource(
    strings = {
      "ISSUE",
      "ENDORSE",
      "SIGN",
      "SURRENDER_FOR_DELIVERY",
      "SURRENDER_FOR_AMENDMENT",
      "BLANK_ENDORSE",
      "ENDORSE_TO_ORDER",
      "TRANSFER",
      "SURRENDERED"
    })
  void everyDocumentedEndorsementActionIsAccepted(String actionCode) {
    ObjectNode request = validRequest("SREQ");
    ((ObjectNode) request.path("endorsementChain").path(0)).put("actionCode", actionCode);

    assertConformant(requestChecks("SREQ").get(1), request);
  }

  @Test
  void unknownEndorsementActionIsRejected() {
    ObjectNode request = validRequest("SREQ");
    ((ObjectNode) request.path("endorsementChain").path(0)).put("actionCode", "UNKNOWN");

    assertNonConformant(requestChecks("SREQ").get(1), request);
  }

  @ParameterizedTest
  @MethodSource("codeListProviderPaths")
  void unknownCodeListProviderIsRejectedAtEveryPartyPath(
    Consumer<ObjectNode> invalidProviderSetter) {
    ObjectNode request = validRequest("SREQ");
    invalidProviderSetter.accept(request);

    assertNonConformant(requestChecks("SREQ").get(2), request);
  }

  @Test
  void documentedCodeListProvidersAreAccepted() {
    ObjectNode request = validRequest("SREQ");
    identifyingCode(party(request, "actor")).put("codeListProvider", "DCSA");
    identifyingCode(party(request, "recipient")).put("codeListProvider", "NONE");

    assertConformant(requestChecks("SREQ").get(2), request);
  }

  @ParameterizedTest
  @ValueSource(strings = {"actor", "recipient"})
  void unknownEblPlatformIsRejectedForEveryParty(String partyType) {
    ObjectNode request = validRequest("SREQ");
    party(request, partyType).put("eblPlatform", "NOPE");

    assertNonConformant(requestChecks("SREQ").get(3), request);
  }

  @ParameterizedTest
  @ValueSource(strings = {"WAVE", "BOLE", "SGTD"})
  void documentedEblPlatformsAreAccepted(String eblPlatform) {
    ObjectNode request = validRequest("SREQ");
    party(request, "actor").put("eblPlatform", eblPlatform);
    party(request, "recipient").put("eblPlatform", eblPlatform);

    assertConformant(requestChecks("SREQ").get(3), request);
  }

  @ParameterizedTest
  @ValueSource(strings = {"SWTP", "COD", "SWI"})
  void everyDocumentedReasonCodeIsAccepted(String reasonCode) {
    ObjectNode request = validRequest("SREQ");
    request.put("reasonCode", reasonCode);

    assertConformant(requestChecks("SREQ").get(4), request);
  }

  @Test
  void unknownReasonCodeIsRejected() {
    ObjectNode request = validRequest("SREQ");
    request.put("reasonCode", "UNKNOWN");

    assertNonConformant(requestChecks("SREQ").get(4), request);
  }

  @ParameterizedTest
  @ValueSource(strings = {"SURR", "SREJ"})
  void documentedSurrenderResponseActionsAreAccepted(String action) {
    ObjectNode response = OBJECT_MAPPER.createObjectNode().put("action", action);

    assertConformant(SurrenderChecks.surrenderResponseContentChecks().getFirst(), response);
  }

  @Test
  void unknownSurrenderResponseActionIsRejected() {
    ObjectNode response = OBJECT_MAPPER.createObjectNode().put("action", "UNKNOWN");

    assertNonConformant(SurrenderChecks.surrenderResponseContentChecks().getFirst(), response);
  }

  @Test
  void createsRequestAndResponseRuntimeChecks() {
    UUID exchangeId = UUID.randomUUID();

    assertTrue(SurrenderChecks.surrenderRequestChecks(exchangeId, "3.0.0", "SREQ").isRelevant());
    assertTrue(SurrenderChecks.surrenderResponseChecks(exchangeId, "3.0.0").isRelevant());
  }

  private static Stream<Arguments> codeListProviderPaths() {
    return Stream.of(
      Arguments.of(
        (Consumer<ObjectNode>)
          request ->
            identifyingCode(party(request, "actor"))
              .put("codeListProvider", "UNKNOWN")),
      Arguments.of(
        (Consumer<ObjectNode>)
          request ->
            identifyingCode(party(request, "recipient"))
              .put("codeListProvider", "UNKNOWN")),
      Arguments.of(
        (Consumer<ObjectNode>)
          request ->
            identifyingCode(representedParty(request, "actor"))
              .put("codeListProvider", "UNKNOWN")),
      Arguments.of(
        (Consumer<ObjectNode>)
          request ->
            identifyingCode(representedParty(request, "recipient"))
              .put("codeListProvider", "UNKNOWN")));
  }

  private static List<JsonContentCheck> requestChecks(String expectedRequestCode) {
    return SurrenderChecks.surrenderRequestContentChecks(expectedRequestCode);
  }

  private static ObjectNode validRequest(String requestCode) {
    ObjectNode request =
      OBJECT_MAPPER
        .createObjectNode()
        .put("surrenderRequestCode", requestCode)
        .put("reasonCode", "SWTP");
    ObjectNode link = request.putArray("endorsementChain").addObject();
    link.put("actionCode", "ISSUE");
    addParty(link, "actor", "WAVE");
    addParty(link, "recipient", "BOLE");
    return request;
  }

  private static void addParty(ObjectNode link, String partyType, String eblPlatform) {
    ObjectNode party = link.putObject(partyType).put("eblPlatform", eblPlatform);
    identifyingCode(party).put("codeListProvider", "DCSA");
    ObjectNode representedParty = party.putObject("representedParty");
    identifyingCode(representedParty).put("codeListProvider", "WAVE");
  }

  private static ObjectNode party(ObjectNode request, String partyType) {
    return (ObjectNode) request.path("endorsementChain").path(0).path(partyType);
  }

  private static ObjectNode representedParty(ObjectNode request, String partyType) {
    return (ObjectNode) party(request, partyType).path("representedParty");
  }

  private static ObjectNode identifyingCode(ObjectNode party) {
    if (!party.has("identifyingCodes")) {
      return party.putArray("identifyingCodes").addObject();
    }
    return (ObjectNode) party.path("identifyingCodes").path(0);
  }

  private static void assertConformant(JsonContentCheck check, ObjectNode body) {
    assertTrue(check.validate(body).isConformant());
  }

  private static void assertNonConformant(JsonContentCheck check, ObjectNode body) {
    assertFalse(check.validate(body).isConformant());
  }
}


