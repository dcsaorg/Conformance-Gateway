package org.dcsa.conformance.standards.booking.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import lombok.With;
import org.dcsa.conformance.core.party.ScenarioParameters;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DynamicScenarioParameters(
    @NonNull ScenarioType scenarioType,
    String carrierBookingRequestReference,
    String carrierBookingReference,
    @Deprecated JsonNode booking,
    @Deprecated JsonNode updatedBooking)
    implements ScenarioParameters {

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, DynamicScenarioParameters.class);
  }
}
