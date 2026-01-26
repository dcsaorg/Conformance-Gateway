package org.dcsa.conformance.end;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.end.party.EndorsementChainRole;

public class EndorsementChainStandard extends AbstractStandard {
  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";
  public static final EndorsementChainStandard INSTANCE = new EndorsementChainStandard();

  protected EndorsementChainStandard() {
    super("Ebl Endorsement Chain");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(Map.entry("3.0.0", new TreeSet<>(Set.of(SCENARIO_SUITE_CONFORMANCE)))));
  }

  @Override
  public Map<String, Map<String, SortedMap<String, SortedSet<String>>>>
      getEndpointUrisAndMethodsByScenarioSuiteAndRoleName() {
    Map<String, SortedMap<String, SortedSet<String>>> endpointUrisAndMethodsByRoleName =
        Map.ofEntries(
            Map.entry(
                EndorsementChainRole.PROVIDER.getConfigName(),
                new TreeMap<>(
                    Map.ofEntries(Map.entry("/arrival-notices", new TreeSet<>(Set.of("GET")))))),
            Map.entry(EndorsementChainRole.CARRIER.getConfigName(), new TreeMap<>()));
    return Map.ofEntries(Map.entry(SCENARIO_SUITE_CONFORMANCE, endpointUrisAndMethodsByRoleName));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return null;
  }

  @Override
  protected Set<String> getExternalPartyRoleNamesAllowingEmptyUrl() {
    return Set.of();
  }
}

