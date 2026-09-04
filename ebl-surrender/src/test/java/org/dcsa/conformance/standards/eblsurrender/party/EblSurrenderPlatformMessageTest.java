package org.dcsa.conformance.standards.eblsurrender.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.eblsurrender.action.SupplyScenarioParametersAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EblSurrenderPlatformMessageTest {

  private static final String API_VERSION = "3.0.0";
  private static final Instant SURRENDER_ACTION_DATE_TIME = Instant.parse("2026-08-31T12:00:00Z");
  private static final JsonSchemaValidator REQUEST_SCHEMA =
    JsonSchemaValidator.getInstance(
      "/standards/eblsurrender/schemas/EBL_SUR_v3.0.0.yaml", "SurrenderRequestDetails");
  private static final JsonSchemaValidator RESPONSE_SCHEMA =
    JsonSchemaValidator.getInstance(
      "/standards/eblsurrender/schemas/EBL_SUR_v3.0.0.yaml", "SurrenderRequestAnswer");

  @ParameterizedTest
  @CsvSource({
    "false, SREQ, SURRENDER_FOR_DELIVERY",
    "true, AREQ, SURRENDER_FOR_AMENDMENT"
  })
  void generatedRequestMatchesScenarioAndSchema(
    boolean forAmendment, String expectedRequestCode, String expectedActionCode) {
    SuppliedScenarioParameters parameters =
      new SuppliedScenarioParameters(
        "TDR-1",
        party("Issue To", "WAVE"),
        party("Carrier", "WAVE"),
        party("Surrenderee", "BOLE"));

    JsonNode request =
      EblSurrenderPlatform.createSurrenderRequestBody(
        API_VERSION, parameters, "SRR-1", forAmendment, SURRENDER_ACTION_DATE_TIME);

    assertEquals("SRR-1", request.path("surrenderRequestReference").asText());
    assertEquals("TDR-1", request.path("transportDocumentReference").asText());
    assertEquals(expectedRequestCode, request.path("surrenderRequestCode").asText());
    assertFalse(request.has("reasonCode"));
    assertFalse(request.has("comments"));

    JsonNode issueLink = request.path("endorsementChain").path(0);
    assertEquals("ISSUE", issueLink.path("actionCode").asText());
    assertEquals(
      SURRENDER_ACTION_DATE_TIME.minusSeconds(60).toString(),
      issueLink.path("actionDateTime").asText());
    assertEquals("Carrier", issueLink.path("actor").path("partyName").asText());
    assertEquals("Issue To", issueLink.path("recipient").path("partyName").asText());

    JsonNode surrenderLink = request.path("endorsementChain").path(1);
    assertEquals(expectedActionCode, surrenderLink.path("actionCode").asText());
    assertEquals(
      SURRENDER_ACTION_DATE_TIME.toString(),
      surrenderLink.path("actionDateTime").asText());
    assertEquals("Surrenderee", surrenderLink.path("actor").path("partyName").asText());
    assertEquals("Carrier", surrenderLink.path("recipient").path("partyName").asText());

    assertTrue(REQUEST_SCHEMA.validate(request.toString()).isEmpty());
  }

  @Test
  void supplyScenarioParametersExampleProducesAValidRequest() {
    JsonNode example =
      new SupplyScenarioParametersAction("Carrier", null).getJsonForHumanReadablePrompt();
    JsonNode request =
      EblSurrenderPlatform.createSurrenderRequestBody(
        API_VERSION,
        SuppliedScenarioParameters.fromJson(example),
        "SRR-1",
        false,
        SURRENDER_ACTION_DATE_TIME);

    assertTrue(REQUEST_SCHEMA.validate(request.toString()).isEmpty());
  }

  @Test
  void generatedResponseMatchesSchema() {
    JsonNode response = EblSurrenderCarrier.createSurrenderResponseBody("SRR-1");

    assertEquals("SRR-1", response.path("surrenderRequestReference").asText());
    assertEquals("SURR", response.path("action").asText());
    assertTrue(RESPONSE_SCHEMA.validate(response.toString()).isEmpty());
  }

  private static ObjectNode party(String partyName, String eblPlatform) {
    ObjectNode party =
      OBJECT_MAPPER
        .createObjectNode()
        .put("partyName", partyName)
        .put("eblPlatform", eblPlatform);
    party
      .putArray("identifyingCodes")
      .addObject()
      .put("codeListProvider", "DCSA")
      .put("partyCode", "PARTY-CODE");
    return party;
  }
}


