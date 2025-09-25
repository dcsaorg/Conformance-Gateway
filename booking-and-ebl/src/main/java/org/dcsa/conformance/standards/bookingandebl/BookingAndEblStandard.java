package org.dcsa.conformance.standards.bookingandebl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    Map<String, Map<String, SortedMap<String, SortedSet<String>>>> bookingResults =
        BookingStandard.INSTANCE.getEndpointUrisAndMethodsByScenarioSuiteAndRoleName();
    Map<String, Map<String, SortedMap<String, SortedSet<String>>>> eblResults =
        EblStandard.INSTANCE.getEndpointUrisAndMethodsByScenarioSuiteAndRoleName();

    Map<String, SortedMap<String, SortedSet<String>>> mergedRoles = new HashMap<>();

    for (var scenarioSuite : bookingResults.values()) {
      for (var roleEntry : scenarioSuite.entrySet()) {
        String roleName = roleEntry.getKey();
        SortedMap<String, SortedSet<String>> endpoints = roleEntry.getValue();

        mergedRoles.computeIfAbsent(roleName, k -> new TreeMap<>());
        for (var endpointEntry : endpoints.entrySet()) {
          String uri = endpointEntry.getKey();
          SortedSet<String> methods = endpointEntry.getValue();

          mergedRoles.get(roleName).computeIfAbsent(uri, k -> new TreeSet<>()).addAll(methods);
        }
      }
    }

    for (var scenarioSuite : eblResults.values()) {
      for (var roleEntry : scenarioSuite.entrySet()) {
        String roleName = roleEntry.getKey();
        SortedMap<String, SortedSet<String>> endpoints = roleEntry.getValue();

        mergedRoles.computeIfAbsent(roleName, k -> new TreeMap<>());
        for (var endpointEntry : endpoints.entrySet()) {
          String uri = endpointEntry.getKey();
          SortedSet<String> methods = endpointEntry.getValue();

          mergedRoles.get(roleName).computeIfAbsent(uri, k -> new TreeSet<>()).addAll(methods);
        }
      }
    }

    Map<String, Map<String, SortedMap<String, SortedSet<String>>>> result = new HashMap<>();
    result.put(BookingAndEblScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE, mergedRoles);

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
