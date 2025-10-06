package org.dcsa.conformance.standards.booking;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.booking.party.BookingRole;

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
                        BookingScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE)))));
  }

  @Override
  public Map<String, Map<String, SortedMap<String, SortedSet<String>>>>
      getEndpointUrisAndMethodsByScenarioSuiteAndRoleName() {
    Map<String, SortedMap<String, SortedSet<String>>> endpointUrisAndMethodsByRoleName =
        Map.ofEntries(
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
    return Map.ofEntries(
        Map.entry(
            BookingScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE,
            endpointUrisAndMethodsByRoleName));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new BookingComponentFactory(getName(), standardVersion, scenarioSuite);
  }

  @Override
  protected Set<String> getExternalPartyRoleNamesAllowingEmptyUrl() {
    return Set.of(BookingRole.SHIPPER.getConfigName());
  }
}
