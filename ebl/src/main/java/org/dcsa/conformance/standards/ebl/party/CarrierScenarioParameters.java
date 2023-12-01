package org.dcsa.conformance.standards.ebl.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record CarrierScenarioParameters(String carrierBookingReference, String commoditySubreference) {
  public ObjectNode toJson() {
    return new ObjectMapper()
        .createObjectNode()
        .put("carrierBookingReference", carrierBookingReference())
        .put("commoditySubreference", commoditySubreference());
  }

  public static CarrierScenarioParameters fromJson(JsonNode jsonNode) {
    return new CarrierScenarioParameters(
      jsonNode.path("carrierBookingReference").asText(),
      jsonNode.path("commoditySubreference").asText()
    );
  }
}
