package org.dcsa.conformance.standards.tnt;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.tnt.party.TntRole;

public class TntStandard extends AbstractStandard {
  public static final TntStandard INSTANCE = new TntStandard();
  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private TntStandard() {
    super("TnT");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(Map.entry("2.2.0", new TreeSet<>(Set.of(SCENARIO_SUITE_CONFORMANCE)))));
  }

  @Override
  public Map<String, Map<String, SortedMap<String, SortedSet<String>>>>
      getEndpointUrisAndMethodsByScenarioSuiteAndRoleName() {
    return Map.ofEntries(
        Map.entry(
            SCENARIO_SUITE_CONFORMANCE,
            Map.ofEntries(
                Map.entry(
                    TntRole.PUBLISHER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(Map.entry("/v2/events", new TreeSet<>(Set.of("GET")))))),
                Map.entry(TntRole.SUBSCRIBER.getConfigName(), new TreeMap<>()))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new TntComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
