package org.dcsa.conformance.standards.booking.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record DynamicScenarioParameters(
    String carrierBookingRequestReference, String carrierBookingReference) {
  public ObjectNode toJson() {
    ObjectNode dspNode = new ObjectMapper().createObjectNode();
    if (carrierBookingRequestReference != null) {
      dspNode.put("carrierBookingRequestReference", carrierBookingRequestReference);
    }
    if (carrierBookingReference != null) {
      dspNode.put("carrierBookingReference", carrierBookingReference);
    }
    return dspNode
        .put("carrierBookingRequestReference", carrierBookingRequestReference())
        .put("carrierBookingReference", carrierBookingReference());
  }

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    ObjectNode dspNode = (ObjectNode) jsonNode;
    return new DynamicScenarioParameters(
        dspNode.has("carrierBookingRequestReference")
            ? dspNode.get("carrierBookingRequestReference").asText()
            : null,
        dspNode.has("carrierBookingReference")
            ? dspNode.get("carrierBookingReference").asText()
            : null);
  }
}
