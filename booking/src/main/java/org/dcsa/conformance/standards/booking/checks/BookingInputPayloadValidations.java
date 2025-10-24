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
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.ConformanceError;
import org.dcsa.conformance.core.check.ConformanceErrorSeverity;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standardscommons.party.BookingDynamicScenarioParameters;

@UtilityClass
public class BookingInputPayloadValidations {

  public static Set<String> validateBookingSchema(
      JsonNode bookingNode, JsonSchemaValidator schemaValidator) {
    return schemaValidator.validate(bookingNode);
  }

  public static Set<String> validateBookingContent(
      JsonNode bookingNode, Supplier<BookingDynamicScenarioParameters> dspSupplier) {

    List<JsonContentCheck> contentChecks = new ArrayList<>(STATIC_BOOKING_CHECKS);
    contentChecks.addAll(generateScenarioRelatedChecks(dspSupplier));

    return contentChecks.stream()
        .filter(JsonContentCheck::isRelevant)
        .flatMap(
            check -> {
              ConformanceCheckResult result = check.validate(bookingNode);
              return switch (result) {
                case ConformanceCheckResult.SimpleErrors(var errors) -> errors.stream();
                case ConformanceCheckResult.ErrorsWithRelevance(var errors) ->
                    errors.stream()
                        .filter(
                            conformanceError ->
                                !ConformanceErrorSeverity.IRRELEVANT.equals(
                                    conformanceError.severity()))
                        .map(ConformanceError::message);
              };
            })
        .collect(Collectors.toSet());
  }
}
