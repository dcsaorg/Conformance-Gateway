package org.dcsa.conformance.end.party;

import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndorsementChainProviderTest {

  private static final JsonSchemaValidator SCHEMA_VALIDATOR =
    JsonSchemaValidator.getInstance(
      "/standards/end/schemas/end-v3.0.0-openapi.yaml", "endorsementChains");

  @Test
  void responseMessageMatchesSchemaAndEchoesRequestedTdrAndTdsr() {
    ConformanceRequest request =
      request(
        "https://provider.example/endorsement-chains/TDR%20WITH%20SPACE",
        Map.of("transportDocumentSubReference", List.of("TDSR-1")));

    var responseBody = EndorsementChainProvider.createResponseBody(request, "300");

    assertEquals("TDR WITH SPACE", responseBody.path(0).path("transportDocumentReference").asText());
    assertEquals(
      "TDSR-1", responseBody.path(0).path("transportDocumentSubReference").asText());
    assertTrue(
      SCHEMA_VALIDATOR.validate(responseBody).isEmpty(),
      () -> "Schema errors: " + SCHEMA_VALIDATOR.validate(responseBody));
  }

  @Test
  void responseMessageUsesSchemaValidTdsrWhenRequestHasNoTdsrFilter() {
    var responseBody =
      EndorsementChainProvider.createResponseBody(
        request("https://provider.example/endorsement-chains/HHL71800000", Map.of()), "300");

    assertEquals(
      "HHL71800000", responseBody.path(0).path("transportDocumentReference").asText());
    assertTrue(
      SCHEMA_VALIDATOR.validate(responseBody).isEmpty(),
      () -> "Schema errors: " + SCHEMA_VALIDATOR.validate(responseBody));
  }

  private static ConformanceRequest request(
    String url, Map<String, ? extends List<String>> queryParams) {
    return new ConformanceRequest(
      "GET",
      url,
      queryParams,
      new ConformanceMessage(
        "Consumer",
        "Consumer",
        "Provider",
        "Provider",
        Map.of(),
        new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()),
        0));
  }
}

