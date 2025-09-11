package org.dcsa.conformance.standards.bookingandebl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.booking.BookingStandard;
import org.dcsa.conformance.standards.bookingandebl.party.BookingAndEblRole;
import org.dcsa.conformance.standards.ebl.EblStandard;

public class BookingAndEblStandard extends AbstractStandard {

  public static final BookingAndEblStandard INSTANCE = new BookingAndEblStandard();

  private BookingAndEblStandard() {
    super("Booking + eBL");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(
            Map.entry(
                "2.0.0-+-3.0.0",
                new TreeSet<>(
                    Set.of(BookingAndEblScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE)))));
  }

  @Override
  public Map<String, Map<String, SortedMap<String, SortedSet<String>>>>
      getEndpointUrisAndMethodsByScenarioSuiteAndRoleName() {
    Map<String, Map<String, SortedMap<String, SortedSet<String>>>> result = new HashMap<>();

    result.putAll(BookingStandard.INSTANCE.getEndpointUrisAndMethodsByScenarioSuiteAndRoleName());
    result.putAll(EblStandard.INSTANCE.getEndpointUrisAndMethodsByScenarioSuiteAndRoleName());

    return result;
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new BookingAndEblComponentFactory(getName(), standardVersion, scenarioSuite);
  }

  @Override
  protected Set<String> getExternalPartyRoleNamesAllowingEmptyUrl() {
    return Set.of(BookingAndEblRole.SHIPPER.getConfigName());
  }
}
