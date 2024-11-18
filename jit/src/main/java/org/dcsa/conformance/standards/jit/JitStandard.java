package org.dcsa.conformance.standards.jit;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.jit.party.JitRole;

public class JitStandard extends AbstractStandard {
  public static final JitStandard INSTANCE = new JitStandard();
  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private JitStandard() {
    super("JIT");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(Map.entry("1.2.0", new TreeSet<>(Set.of(SCENARIO_SUITE_CONFORMANCE)))));
  }

  @Override
  public Map<String, Map<String, SortedMap<String, SortedSet<String>>>>
      getEndpointUrisAndMethodsByScenarioSuiteAndRoleName() {
    return Map.ofEntries(
        Map.entry(
            SCENARIO_SUITE_CONFORMANCE,
            Map.ofEntries(
                Map.entry(
                    JitRole.PUBLISHER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            Map.entry(
                                "/v2/port-call-services/{portCallServiceID}",
                                new TreeSet<>(Set.of("GET")))))),
                Map.entry(JitRole.SUBSCRIBER.getConfigName(), new TreeMap<>()))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new JitComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
