package org.dcsa.conformance.standards.booking.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.With;

@With
public record DynamicScenarioParameters(
    String carrierBookingRequestReference,
    String carrierBookingReference,
    BookingState bookingStatus,
    BookingState amendedBookingStatus) {
  public ObjectNode toJson() {
    ObjectNode dspNode = new ObjectMapper().createObjectNode();
    if (carrierBookingRequestReference != null) {
      dspNode.put("carrierBookingRequestReference", carrierBookingRequestReference);
    }
    if (carrierBookingReference != null) {
      dspNode.put("carrierBookingReference", carrierBookingReference);
    }
    if (bookingStatus != null) {
      dspNode.put("bookingStatus", bookingStatus.wireName());
    }
    if (amendedBookingStatus != null) {
      dspNode.put("amendedBookingStatus", amendedBookingStatus.wireName());
    }
    return dspNode;
  }

  private static BookingState bookingState(String value) {
    if (value == null) {
      return null;
    }
    return BookingState.fromWireName(value);
  }

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    ObjectNode dspNode = (ObjectNode) jsonNode;
    return new DynamicScenarioParameters(
        dspNode.path("carrierBookingRequestReference").asText(null),
        dspNode.path("carrierBookingReference").asText(null),
        bookingState(dspNode.path("bookingStatus").asText(null)),
        bookingState(dspNode.path("amendedBookingStatus").asText(null))
      );
  }
}
