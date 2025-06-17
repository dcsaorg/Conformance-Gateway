package org.dcsa.conformance.standards.cs;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.cs.party.CsRole;

public class CsStandard extends AbstractStandard {
  public static final CsStandard INSTANCE = new CsStandard();
  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private CsStandard() {
    super("CS");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(Map.entry("1.0.0", new TreeSet<>(Set.of(SCENARIO_SUITE_CONFORMANCE)))));
  }

  @Override
  public Map<String, Map<String, SortedMap<String, SortedSet<String>>>>
      getEndpointUrisAndMethodsByScenarioSuiteAndRoleName() {
    return Map.ofEntries(
        Map.entry(
            SCENARIO_SUITE_CONFORMANCE,
            Map.ofEntries(
                Map.entry(
                    CsRole.PUBLISHER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            Map.entry("/v1/point-to-point-routes", new TreeSet<>(Set.of("GET"))),
                            Map.entry("/v1/port-schedules", new TreeSet<>(Set.of("GET"))),
                            Map.entry("/v1/vessel-schedules", new TreeSet<>(Set.of("GET")))))),
                Map.entry(CsRole.SUBSCRIBER.getConfigName(), new TreeMap<>()))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new CsComponentFactory(getName(), standardVersion, scenarioSuite);
  }

  @Override
  protected Set<String> getExternalPartyRoleNamesAllowingEmptyUrl() {
    return Set.of(CsRole.SUBSCRIBER.getConfigName());
  }
}
