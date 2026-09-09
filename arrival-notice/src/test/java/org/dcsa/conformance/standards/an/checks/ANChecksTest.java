package org.dcsa.conformance.standards.an.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.standards.an.party.DynamicScenarioParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ANChecksTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String FIXTURE =
    "/standards/an/messages/arrivalnotice-api-100-get-basic-response.json";

  @Test
  void exposesEveryDocumentedRuleAndAcceptsACompletePayload() throws IOException {
    JsonNode body = validBody();

    assertChecksPass(41, ScenarioType.BASIC, body);
    assertChecksPass(47, ScenarioType.FREE_TIME, body);
    assertChecksPass(48, ScenarioType.FREIGHTED, body);
    ANChecks.getANPostPayloadChecks(UUID.randomUUID(), "1.0.0", ScenarioType.BASIC.name());
    ANChecks.getANGetResponseChecks(
      UUID.randomUUID(),
      "1.0.0",
      () ->
        new DynamicScenarioParameters(
          Map.of(), ScenarioType.BASIC.name(), null, null, null));
  }

  @Test
  void rejectsMissingOrEmptyOccurrencesButIfPresentRulesAreVacuouslyTrue() {
    ObjectNode missing = MAPPER.createObjectNode();
    List<JsonContentCheck> checks = ANChecks.payloadChecksForScenario(ScenarioType.BASIC.name());
    long passing = checks.stream().filter(check -> passes(check, missing)).count();
    assertEquals(2, passing, "Only the two if-present rules should pass without their objects");

    ObjectNode empty = MAPPER.createObjectNode();
    empty.putArray("arrivalNotices");
    assertFalse(passes(checks.getFirst(), empty));
    empty.putNull("arrivalNotices");
    assertFalse(passes(checks.getFirst(), empty));
  }

  @Test
  void representativeOccurrencesMayBeSplitAcrossSiblingItems() throws IOException {
    ObjectNode body = validBody();
    ObjectNode first = firstArrivalNotice(body);
    ObjectNode second = first.deepCopy();
    first.putArray("carrierContactInformation").addObject().put("name", "Contact");
    second.putArray("carrierContactInformation").addObject().put("email", "ops@example.com");
    ((ArrayNode) body.path("arrivalNotices")).add(second);

    List<JsonContentCheck> checks = ANChecks.payloadChecksForScenario(ScenarioType.BASIC.name());
    assertTrue(passes(checks.get(5), body));
    assertTrue(passes(checks.get(6), body));
  }

  @Test
  void validatesEveryPresentPortFacilityAndAddress() throws IOException {
    ObjectNode body = validBody();
    ObjectNode port =
      (ObjectNode) firstArrivalNotice(body).path("transport").path("portOfDischarge");
    port.set("facility", MAPPER.createObjectNode());
    port.set("address", MAPPER.createObjectNode());

    List<JsonContentCheck> checks = ANChecks.payloadChecksForScenario(ScenarioType.BASIC.name());
    assertFalse(passes(checks.get(19), body));
    assertFalse(passes(checks.get(20), body));

    ((ObjectNode) port.path("facility")).put("facilityName", "Terminal");
    ((ObjectNode) port.path("address")).putArray("addressLines").add("Terminal road");
    assertTrue(passes(checks.get(19), body));
    assertTrue(passes(checks.get(20), body));
  }

  @Test
  void coversAlternativeAndInvalidScalarBranches() throws IOException {
    ObjectNode body = validBody();
    ObjectNode notice = firstArrivalNotice(body);
    ObjectNode contact =
      (ObjectNode) notice.path("carrierContactInformation").path(0);
    contact.put("phone", "");
    assertTrue(passes(ANChecks.payloadChecksForScenario("BASIC").get(5), body));

    ObjectNode transport = (ObjectNode) notice.path("transport");
    transport.remove("portOfDischargeArrivalDate");
    transport.putObject("placeOfDeliveryArrivalDate").put("value", "2024-03-12");
    assertTrue(passes(ANChecks.payloadChecksForScenario("BASIC").get(16), body));

    ObjectNode port = (ObjectNode) transport.path("portOfDischarge");
    port.remove("UNLocationCode");
    port.remove("facility");
    assertTrue(passes(ANChecks.payloadChecksForScenario("BASIC").get(18), body));
    port.remove("address");
    assertFalse(passes(ANChecks.payloadChecksForScenario("BASIC").get(18), body));

    ObjectNode packaging =
      (ObjectNode)
        notice.path("consignmentItems").path(0).path("cargoItems").path(0).path("outerPackaging");
    packaging.put("packageCode", "");
    packaging.put("IMOPackagingCode", "4G");
    assertTrue(passes(ANChecks.payloadChecksForScenario("BASIC").get(39), body));
    packaging.put("IMOPackagingCode", "");
    assertTrue(passes(ANChecks.payloadChecksForScenario("BASIC").get(39), body));

    notice.put("carrierCodeListProvider", "INVALID");
    assertFalse(passes(ANChecks.payloadChecksForScenario("BASIC").get(3), body));
    notice.put("carrierCode", "");
    assertFalse(passes(ANChecks.payloadChecksForScenario("BASIC").get(2), body));
    packaging.remove("numberOfPackages");
    assertFalse(passes(ANChecks.payloadChecksForScenario("BASIC").get(40), body));
  }

  @Test
  void coversAllRemainingAlternativeAndShortCircuitBranches() throws IOException {
    List<JsonContentCheck> checks = ANChecks.payloadChecksForScenario(ScenarioType.BASIC.name());

    ObjectNode nullListBody = MAPPER.createObjectNode();
    nullListBody.putNull("arrivalNotices");
    assertFalse(passes(checks.get(1), nullListBody));

    ObjectNode body = validBody();
    ObjectNode notice = firstArrivalNotice(body);
    notice.put("carrierCodeListProvider", "");
    assertFalse(passes(checks.get(3), body));

    ObjectNode partyContact =
      (ObjectNode) notice.path("documentParties").path(0).path("partyContactDetails").path(0);
    partyContact.put("phone", "");
    assertTrue(passes(checks.get(11), body));
    notice
      .path("documentParties")
      .forEach(
        party ->
          party.path("partyContactDetails")
            .forEach(
              contact -> {
                ((ObjectNode) contact).put("phone", "");
                ((ObjectNode) contact).put("email", "");
              }));
    assertFalse(passes(checks.get(11), body));

    ObjectNode transport = (ObjectNode) notice.path("transport");
    transport.remove("portOfDischargeArrivalDate");
    transport.remove("placeOfDeliveryArrivalDate");
    assertFalse(passes(checks.get(16), body));

    ObjectNode port = (ObjectNode) transport.path("portOfDischarge");
    port.remove("UNLocationCode");
    port.remove("address");
    assertTrue(passes(checks.get(18), body));

    ObjectNode facility = (ObjectNode) port.path("facility");
    facility.put("facilityCode", "CODE");
    facility.put("facilityCodeListProvider", "INVALID");
    assertFalse(passes(checks.get(19), body));

    ObjectNode address = port.putObject("address");
    address.put("street", "");
    address.putArray("addressLines").add("Terminal road");
    assertTrue(passes(checks.get(20), body));

    ObjectNode packaging =
      (ObjectNode)
        notice.path("consignmentItems").path(0).path("cargoItems").path(0).path("outerPackaging");
    packaging.put("packageCode", "");
    packaging.put("IMOPackagingCode", "");
    packaging.put("description", "");
    assertFalse(passes(checks.get(39), body));
  }

  @Test
  void profileChecksRejectInvalidValues() throws IOException {
    ObjectNode freeTimeBody = validBody();
    ObjectNode freeTime = (ObjectNode) firstArrivalNotice(freeTimeBody).path("freeTimes").path(0);
    freeTime.putArray("typeCodes").add("INVALID");
    freeTime.putArray("ISOEquipmentCodes").add("");
    freeTime.putArray("equipmentReferences").add("");
    freeTime.put("duration", 0);
    freeTime.put("timeUnit", "INVALID");
    List<JsonContentCheck> freeTimeChecks =
      ANChecks.payloadChecksForScenario(ScenarioType.FREE_TIME.name());
    freeTimeChecks.subList(42, 47).forEach(check -> assertFalse(passes(check, freeTimeBody)));

    ObjectNode freightedBody = validBody();
    ObjectNode charge = (ObjectNode) firstArrivalNotice(freightedBody).path("charges").path(0);
    charge.put("chargeName", "");
    charge.put("currencyAmount", 0);
    charge.put("currencyCode", "");
    charge.put("paymentTermCode", "INVALID");
    charge.put("unitPrice", 0);
    charge.put("quantity", 0);
    List<JsonContentCheck> freightedChecks =
      ANChecks.payloadChecksForScenario(ScenarioType.FREIGHTED.name());
    freightedChecks.subList(42, 48).forEach(check -> assertFalse(passes(check, freightedBody)));
  }

  @Test
  void allCheckDescriptionsUseSingleQuotesForAttributes() {
    ANChecks.payloadChecksForScenario(ScenarioType.FREE_TIME.name()).forEach(
      check -> assertFalse(check.description().contains("`")));
    ANChecks.payloadChecksForScenario(ScenarioType.FREIGHTED.name()).forEach(
      check -> assertFalse(check.description().contains("`")));
  }

  @Test
  void utilityConstructorCannotBeUsed() throws Exception {
    var constructor = ANChecks.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    InvocationTargetException exception =
      assertThrows(InvocationTargetException.class, constructor::newInstance);
    assertTrue(exception.getCause() instanceof UnsupportedOperationException);
  }

  private static void assertChecksPass(int expectedCount, ScenarioType type, JsonNode body) {
    List<JsonContentCheck> checks = ANChecks.payloadChecksForScenario(type.name());
    assertEquals(expectedCount, checks.size());
    checks.forEach(check -> assertTrue(passes(check, body), check.description()));
  }

  private static boolean passes(JsonContentCheck check, JsonNode body) {
    return check.validate(body).getErrorMessages().isEmpty();
  }

  private static ObjectNode validBody() throws IOException {
    try (var stream = ANChecksTest.class.getResourceAsStream(FIXTURE)) {
      return (ObjectNode) MAPPER.readTree(stream);
    }
  }

  private static ObjectNode firstArrivalNotice(ObjectNode body) {
    return (ObjectNode) body.path("arrivalNotices").path(0);
  }
}




