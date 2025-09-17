package org.dcsa.conformance.standards.booking.party;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.With;
import org.dcsa.conformance.core.party.ScenarioParameters;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingDynamicScenarioParameters(
    String bookingScenarioType,
    String carrierBookingRequestReference,
    String carrierBookingReference)
    implements ScenarioParameters {

  public static BookingDynamicScenarioParameters fromJson(JsonNode jsonNode) {
    return new BookingDynamicScenarioParameters(
        jsonNode.path("bookingScenarioType").asText(null),
        jsonNode.path("carrierBookingRequestReference").asText(null),
        jsonNode.path("carrierBookingReference").asText(null));
  }
}
