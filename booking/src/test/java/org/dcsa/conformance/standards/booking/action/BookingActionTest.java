package org.dcsa.conformance.standards.booking.action;

import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
  void carrierNotificationActionsAreSkippableOnlyForCarrier(BookingAction action) {
    assertEquals(Set.of(BookingRole.CARRIER.getConfigName()), action.skippableForRoles());
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
