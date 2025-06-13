package org.dcsa.conformance.standards.booking.checks;

import static org.dcsa.conformance.standards.booking.checks.BookingChecks.STATIC_BOOKING_CHECKS;
import static org.dcsa.conformance.standards.booking.checks.BookingChecks.generateScenarioRelatedChecks;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.booking.party.DynamicScenarioParameters;

@UtilityClass
public class BookingInputPayloadValidations {

  public static Set<String> validateBookingSchema(
      JsonNode bookingNode, JsonSchemaValidator schemaValidator) {
    return schemaValidator.validate(bookingNode);
  }

  public static Set<String> validateBookingContent(
      JsonNode bookingNode, Supplier<DynamicScenarioParameters> dspSupplier) {
    List<JsonContentCheck> contentChecks = new ArrayList<>(STATIC_BOOKING_CHECKS);
    contentChecks.addAll(generateScenarioRelatedChecks(dspSupplier));
    return contentChecks.stream()
        .flatMap(check -> check.validate(bookingNode).stream())
        .collect(Collectors.toSet());
  }

  public static Set<String> validateBookingScenarioType(
      JsonNode bookingNode, ScenarioType scenarioType) {
    return switch (scenarioType) {
      case REEFER_TEMP_CHANGE, REEFER -> validateBookingReeferType(bookingNode);
      case DG -> validateBookingDgType(bookingNode);
      default -> validateBookingRegularType(bookingNode);
    };
  }

  private static Set<String> validateBookingRegularType(JsonNode bookingNode) {}

  private static Set<String> validateBookingReeferType(JsonNode bookingNode) {}

  private static Set<String> validateBookingDgType(JsonNode bookingNode) {}
}
