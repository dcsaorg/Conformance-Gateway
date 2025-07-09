package org.dcsa.conformance.standards.booking.action;

import static org.junit.jupiter.api.Assertions.*;

import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.junit.jupiter.api.Test;

class BookingActionTest {

  private final BookingAction bookingAction =
      new CarrierSupplyScenarioParametersAction(
          "name",
          ScenarioType.REGULAR,
          "2.0.0",
          JsonSchemaValidator.getInstance(
              "/schemas/dcsaorg-DCSA_BKG-2.0.0-resolved.yaml", "CreateBooking"));

  @Test
  void testEmptyUriReferenceReturnsBaseUri() {
    String[] result = bookingAction.buildFullUris("/base");
    assertArrayEquals(new String[] {"/base"}, result);
  }

  @Test
  void testNullUriReferenceReturnsBaseUri() {
    String[] result = bookingAction.buildFullUris("/base", (String[]) null);
    assertArrayEquals(new String[] {"/base"}, result);
  }

  @Test
  void testSingleUriReference() {
    String[] result = bookingAction.buildFullUris("/base", "/123");
    assertArrayEquals(new String[] {"/base/123"}, result);
  }

  @Test
  void testMultipleUriReferences() {
    String[] result = bookingAction.buildFullUris("/base", "/123", "/cancel");
    assertArrayEquals(new String[] {"/base/123", "/base/cancel"}, result);
  }

  @Test
  void testUriReferenceWithNullsIgnored() {
    String[] result = bookingAction.buildFullUris("/base", null, "/a", null, "/b");
    assertArrayEquals(new String[] {"/base/a", "/base/b"}, result);
  }

  @Test
  void testAllNullUriReferencesReturnsEmptyArray() {
    String[] result = bookingAction.buildFullUris("/base", null, null);
    assertArrayEquals(new String[] {}, result);
  }

  @Test
  void testUriWithTrailingSlash() {
    String[] result = bookingAction.buildFullUris("/base/", "/id");
    assertArrayEquals(new String[] {"/base//id"}, result); // note the double slash
  }

  @Test
  void testUriReferenceWithEmptyString() {
    String[] result = bookingAction.buildFullUris("/base", "");
    assertArrayEquals(new String[] {"/base"}, result);
  }

  @Test
  void testMixedEmptyAndNullUriReferences() {
    String[] result = bookingAction.buildFullUris("/base", "", null, "/x");
    assertArrayEquals(new String[] {"/base", "/base/x"}, result);
  }
}
