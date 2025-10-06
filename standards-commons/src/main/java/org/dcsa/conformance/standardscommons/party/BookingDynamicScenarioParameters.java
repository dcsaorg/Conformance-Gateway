package org.dcsa.conformance.standardscommons.party;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.With;
import org.dcsa.conformance.core.party.ScenarioParameters;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingDynamicScenarioParameters(
    String scenarioType,
    String carrierBookingRequestReference,
    String carrierBookingReference)
    implements ScenarioParameters {

  public static BookingDynamicScenarioParameters fromJson(JsonNode jsonNode) {
    return new BookingDynamicScenarioParameters(
        jsonNode.path("scenarioType").asText(null),
        jsonNode.path("carrierBookingRequestReference").asText(null),
        jsonNode.path("carrierBookingReference").asText(null));
  }
}
