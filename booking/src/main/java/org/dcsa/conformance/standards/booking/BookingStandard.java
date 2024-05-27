package org.dcsa.conformance.standards.booking;

import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;

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
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new BookingComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
