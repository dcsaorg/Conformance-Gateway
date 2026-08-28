package org.dcsa.conformance.standards.booking.action;

import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingActionTest {

  private final BookingAction bookingAction =
    new CarrierSupplyScenarioParametersAction(
      "name",
      ScenarioType.DRY_CARGO,
      "2.0.0",
      JsonSchemaValidator.getInstance(
        "/schemas/dcsaorg-DCSA_BKG-2.0.0-resolved.yaml", "CreateBooking"));

  @ParameterizedTest
  @MethodSource("carrierNotificationActions")
  void carrierNotificationActionsAreCompletableWithoutTrafficButNotSkippable(
      BookingAction action) {
    assertEquals(Set.of(), action.skippableForRoles());
    assertEquals(
        Set.of(BookingRole.CARRIER.getConfigName()),
        action.completableWithoutTrafficForRoles());
  }

  private static ConformanceExchange exchange(int status, String responseBody) {
    var requestMessage = new ConformanceMessage(
      "Shipper", "Shipper", "Carrier", "Carrier", Map.of("API-Version", List.of("2.0.0")),
      new ConformanceMessageBody("{}"), 0);
    var responseMessage = new ConformanceMessage(
      "Carrier", "Carrier", "Shipper", "Shipper", Map.of("API-Version", List.of("2.0.0")),
      new ConformanceMessageBody(responseBody), 0);
    return new ConformanceExchange(
      new ConformanceRequest("POST", "https://example.test/v2/bookings", Map.of(), requestMessage),
      new ConformanceResponse(status, responseMessage));
  }

  private static Stream<BookingAction> carrierNotificationActions() {
    BookingAction previousAction = new CarrierSupplyScenarioParametersAction(
      "Carrier", ScenarioType.DRY_CARGO, "2.0.0", null);
    return Stream.of(
      new UC2_Carrier_RequestUpdateToBookingRequestAction("Carrier", "Shipper", previousAction, null, true),
      new UC4_Carrier_RejectBookingRequestAction("Carrier", "Shipper", previousAction, null, true),
      new UC5_Carrier_ConfirmBookingRequestAction("Carrier", "Shipper", previousAction, null, true),
      new UC6_Carrier_RequestToAmendConfirmedBookingAction("Carrier", "Shipper", previousAction, null, true),
      new UC8_Carrier_ProcessAmendmentAction("Carrier", "Shipper", previousAction, null, true),
      new UC10_Carrier_DeclineBookingAction("Carrier", "Shipper", previousAction, null, null, true),
      new UC12_Carrier_ConfirmBookingCompletedAction("Carrier", "Shipper", previousAction, null, true),
      new UC14CarrierProcessBookingCancellationAction("Carrier", "Shipper", previousAction, null, true));
  }

  @Test
  void testEmptyUriReferenceReturnsBaseUri() {
    String[] result = bookingAction.buildFullUris("/base");
    assertArrayEquals(new String[]{"/base"}, result);
  }

  @Test
  void testNullUriReferenceReturnsBaseUri() {
    String[] result = bookingAction.buildFullUris("/base", (String[]) null);
    assertArrayEquals(new String[]{"/base"}, result);
  }

  @Test
  void testSingleUriReference() {
    String[] result = bookingAction.buildFullUris("/base", "/123");
    assertArrayEquals(new String[]{"/base/123"}, result);
  }

  @Test
  void testMultipleUriReferences() {
    String[] result = bookingAction.buildFullUris("/base", "/123", "/cancel");
    assertArrayEquals(new String[]{"/base/123", "/base/cancel"}, result);
  }

  @Test
  void testUriReferenceWithNullsIgnored() {
    String[] result = bookingAction.buildFullUris("/base", null, "/a", null, "/b");
    assertArrayEquals(new String[]{"/base/a", "/base/b"}, result);
  }

  @Test
  void testAllNullUriReferencesReturnsEmptyArray() {
    String[] result = bookingAction.buildFullUris("/base", null, null);
    assertArrayEquals(new String[]{}, result);
  }

  @Test
  void testUriReferenceWithEmptyString() {
    String[] result = bookingAction.buildFullUris("/base", "  ");
    assertArrayEquals(new String[]{}, result);
  }

  @Test
  void testMixedEmptyAndNullUriReferences() {
    String[] result = bookingAction.buildFullUris("/base", "  ", null, "/x");
    assertArrayEquals(new String[]{"/base/x"}, result);
  }
}
