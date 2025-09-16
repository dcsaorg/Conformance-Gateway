package org.dcsa.conformance.standards.ebl.party;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.With;
import org.dcsa.conformance.core.party.ScenarioParameters;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DynamicScenarioParameters(
    // EBL related parameters
    String eblScenarioType,
    String shippingInstructionsReference,
    String transportDocumentReference,
    JsonNode shippingInstructions,
    JsonNode updatedShippingInstructions,
    boolean newTransportDocumentContent,
    @Deprecated JsonNode transportDocument,
    @Deprecated JsonNode previousTransportDocument,
    // Booking related parameters
    String bookingScenarioType,
    String carrierBookingRequestReference,
    String carrierBookingReference,
    @Deprecated JsonNode booking,
    @Deprecated JsonNode updatedBooking)
    implements ScenarioParameters {

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    return new DynamicScenarioParameters(
        // EBL related parameters
        jsonNode.path("eblScenarioType").asText(null),
        jsonNode.path("shippingInstructionsReference").asText(null),
        jsonNode.path("transportDocumentReference").asText(null),
        jsonNode.path("shippingInstructions"),
        jsonNode.path("updatedShippingInstructions"),
        jsonNode.path("newTransportDocumentContent").asBoolean(false),
        jsonNode.path("transportDocument"),
        jsonNode.path("previousTransportDocument"),
        // Booking related parameters
        jsonNode.path("bookingScenarioType").asText(null),
        jsonNode.path("carrierBookingRequestReference").asText(null),
        jsonNode.path("carrierBookingReference").asText(null),
        jsonNode.path("booking"),
        jsonNode.path("updatedBooking"));
  }
}
