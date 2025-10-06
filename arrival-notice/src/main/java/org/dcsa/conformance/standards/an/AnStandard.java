package org.dcsa.conformance.standards.an;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.an.party.ANRole;

public class AnStandard extends AbstractStandard {
  public static final AnStandard INSTANCE = new AnStandard();
  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private AnStandard() {
    super("AN");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(Map.entry("1.0.0", new TreeSet<>(Set.of(SCENARIO_SUITE_CONFORMANCE)))));
  }


  @Override
  public Map<String, Map<String, SortedMap<String, SortedSet<String>>>>
      getEndpointUrisAndMethodsByScenarioSuiteAndRoleName() {
    Map<String, SortedMap<String, SortedSet<String>>> endpointUrisAndMethodsByRoleName =
        Map.ofEntries(
            Map.entry(
                ANRole.PUBLISHER.getConfigName(),
                new TreeMap<>(
                    Map.ofEntries(Map.entry("/arrival-notices", new TreeSet<>(Set.of("GET")))))),
            Map.entry(
                ANRole.SUBSCRIBER.getConfigName(),
                new TreeMap<>(
                    Map.ofEntries(
                        Map.entry("/arrival-notices", new TreeSet<>(Set.of("POST"))),
                        Map.entry(
                            "/arrival-notice-notifications", new TreeSet<>(Set.of("POST")))))));
    return Map.ofEntries(Map.entry(SCENARIO_SUITE_CONFORMANCE, endpointUrisAndMethodsByRoleName));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new ANComponentFactory(getName(), standardVersion, scenarioSuite);
  }

  @Override
  protected Set<String> getExternalPartyRoleNamesAllowingEmptyUrl() {
    return Set.of(ANRole.SUBSCRIBER.getConfigName());
  }
}
