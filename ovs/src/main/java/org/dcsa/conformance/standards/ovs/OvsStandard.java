package org.dcsa.conformance.standards.ovs;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.ovs.party.OvsRole;

public class OvsStandard extends AbstractStandard {
  public static final OvsStandard INSTANCE = new OvsStandard();
  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private OvsStandard() {
    super("OVS");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(Map.entry("3.0.0", new TreeSet<>(Set.of(SCENARIO_SUITE_CONFORMANCE)))));
  }

  @Override
  public Map<String, Map<String, SortedMap<String, SortedSet<String>>>>
      getEndpointUrisAndMethodsByScenarioSuiteAndRoleName() {
    return Map.ofEntries(
        Map.entry(
            SCENARIO_SUITE_CONFORMANCE,
            Map.ofEntries(
                Map.entry(
                    OvsRole.PUBLISHER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            Map.entry("/v3/service-schedules", new TreeSet<>(Set.of("GET")))))),
                Map.entry(OvsRole.SUBSCRIBER.getConfigName(), new TreeMap<>()))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new OvsComponentFactory(getName(), standardVersion, scenarioSuite);
  }

  @Override
  protected Set<String> getExternalPartyRoleNamesAllowingEmptyUrl() {
    return Set.of(OvsRole.SUBSCRIBER.getConfigName());
  }
}
