package org.dcsa.conformance.standards.booking;

import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.booking.party.BookingRole;

import java.util.*;

public class BookingStandard extends AbstractStandard {
  public static final BookingStandard INSTANCE = new BookingStandard();

  private BookingStandard() {
    super("Booking");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(
            Map.entry(
                "2.0.0",
                new TreeSet<>(
                    Set.of(
                        BookingScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE,
                        BookingScenarioListBuilder.SCENARIO_SUITE_RI)))));
  }

  @Override
  public Map<String, SortedMap<String, SortedSet<String>>> getRoleNameEndpointUriMethods() {
    return Map.ofEntries(
        Map.entry(
            BookingRole.CARRIER.getConfigName(),
            new TreeMap<>(
                Map.ofEntries(
                    Map.entry("/v2/bookings", new TreeSet<>(Set.of("POST"))),
                    Map.entry(
                        "/v2/bookings/{bookingReference}",
                        new TreeSet<>(Set.of("PUT", "GET", "PATCH")))))),
        Map.entry(
            BookingRole.SHIPPER.getConfigName(),
            new TreeMap<>(
                Map.ofEntries(
                    Map.entry("/v2/booking-notifications", new TreeSet<>(Set.of("POST")))))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new BookingComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
