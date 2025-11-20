package org.dcsa.conformance.standards.vgm;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.vgm.party.VgmRole;

public class VgmStandard extends AbstractStandard {

  public static final VgmStandard INSTANCE = new VgmStandard();
  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private VgmStandard() {
    super("VGM");
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
                VgmRole.PRODUCER.getConfigName(),
                new TreeMap<>(
                    Map.ofEntries(Map.entry("/vgm-declarations", new TreeSet<>(Set.of("GET")))))),
            Map.entry(
                VgmRole.CONSUMER.getConfigName(),
                new TreeMap<>(
                    Map.ofEntries(Map.entry("/vgm-declarations", new TreeSet<>(Set.of("POST")))))));
    return Map.ofEntries(Map.entry(SCENARIO_SUITE_CONFORMANCE, endpointUrisAndMethodsByRoleName));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new VgmComponentFactory(getName(), standardVersion, scenarioSuite);
  }

  @Override
  protected Set<String> getExternalPartyRoleNamesAllowingEmptyUrl() {
    return Set.of(VgmRole.CONSUMER.getConfigName());
  }
}
