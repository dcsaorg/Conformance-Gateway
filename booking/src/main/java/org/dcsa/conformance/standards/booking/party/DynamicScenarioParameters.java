package org.dcsa.conformance.standards.booking.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.With;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;

import java.util.function.Function;

@With
public record DynamicScenarioParameters(
    @NonNull
    ScenarioType scenarioType,
    String carrierBookingRequestReference,
    String carrierBookingReference,
    BookingState bookingStatus,
    BookingState amendedBookingStatus,
    BookingCancellationState bookingCancellationStatus,
    JsonNode booking,
    JsonNode updatedBooking
    ) {
  public ObjectNode toJson() {
    ObjectNode dspNode = new ObjectMapper().createObjectNode();
    dspNode.put("scenarioType", scenarioType.name());
    if (carrierBookingRequestReference != null) {
      dspNode.put("carrierBookingRequestReference", carrierBookingRequestReference);
    }
    if (carrierBookingReference != null) {
      dspNode.put("carrierBookingReference", carrierBookingReference);
    }
    if (bookingStatus != null) {
      dspNode.put("bookingStatus", bookingStatus.name());
    }
    if (amendedBookingStatus != null) {
      dspNode.put("amendedBookingStatus", amendedBookingStatus.name());
    }
    if (bookingCancellationStatus != null) {
      dspNode.put("bookingCancellationStatus", bookingCancellationStatus.name());
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
        bookingState(dspNode.path("bookingStatus").asText(null)),
        bookingState(dspNode.path("amendedBookingStatus").asText(null)),
        bookingCancellationState(dspNode.path("bookingCancellationStatus").asText(null)),
        dspNode.path("booking"),
        dspNode.path("updatedBooking")
      );
  }
}
