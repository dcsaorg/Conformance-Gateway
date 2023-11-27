package org.dcsa.conformance.standards.ebl.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record CarrierScenarioParameters(String carrierServiceName, String vesselIMONumber) {
  public ObjectNode toJson() {
    return new ObjectMapper()
        .createObjectNode()
        .put("carrierServiceName", carrierServiceName())
        .put("vesselIMONumber", vesselIMONumber());
  }

  public static CarrierScenarioParameters fromJson(JsonNode jsonNode) {
    ObjectNode cspNode = (ObjectNode) jsonNode;
    return new CarrierScenarioParameters(
        cspNode.get("carrierServiceName").asText(), cspNode.get("vesselIMONumber").asText());
  }
}
