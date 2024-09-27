package org.dcsa.conformance.standards.booking.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Function;
import lombok.NonNull;
import lombok.With;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;

@With
public record DynamicScenarioParameters(
    @NonNull
    ScenarioType scenarioType,
    String carrierBookingRequestReference,
    String carrierBookingReference,
    JsonNode booking,
    JsonNode updatedBooking
    ) {
  public ObjectNode toJson() {
    ObjectNode dspNode = OBJECT_MAPPER.createObjectNode();
    dspNode.put("scenarioType", scenarioType.name());
    if (carrierBookingRequestReference != null) {
      dspNode.put("carrierBookingRequestReference", carrierBookingRequestReference);
    }
    if (carrierBookingReference != null) {
      dspNode.put("carrierBookingReference", carrierBookingReference);
    }
    if (booking != null) {
      dspNode.replace("booking", booking);
    }
    if (updatedBooking != null) {
      dspNode.replace("updatedBooking", updatedBooking);
    }
    return dspNode;
  }

  private static BookingState bookingState(String value) {
    if (value == null) {
      return null;
    }
    return BookingState.valueOf(value);
  }

  private static BookingCancellationState bookingCancellationState(String value) {
    if (value == null) {
      return null;
    }
    return BookingCancellationState.valueOf(value);
  }

  private static <E> E readEnum(String value, Function<String, E> mapper) {
    if (value == null) {
      return null;
    }
    return mapper.apply(value);
  }

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    ObjectNode dspNode = (ObjectNode) jsonNode;
    return new DynamicScenarioParameters(
      readEnum(jsonNode.required("scenarioType").asText(), ScenarioType::valueOf),
        dspNode.path("carrierBookingRequestReference").asText(null),
        dspNode.path("carrierBookingReference").asText(null),
        dspNode.path("booking"),
        dspNode.path("updatedBooking")
      );
  }
}
