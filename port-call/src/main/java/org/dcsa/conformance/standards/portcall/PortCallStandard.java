package org.dcsa.conformance.standards.portcall;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.portcall.party.PortCallRole;

public class PortCallStandard extends AbstractStandard {

  public static final PortCallStandard INSTANCE = new PortCallStandard();
  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  protected PortCallStandard() {
    super("PortCall");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
      Map.ofEntries(Map.entry("2.0.0", new TreeSet<>(Set.of(SCENARIO_SUITE_CONFORMANCE)))));
  }

  @Override
  public Map<String, Map<String, SortedMap<String, SortedSet<String>>>> getEndpointUrisAndMethodsByScenarioSuiteAndRoleName() {
    Map<String, SortedMap<String, SortedSet<String>>> endpointUrisAndMethodsByRoleName =
      Map.ofEntries(
        Map.entry(
          PortCallRole.PUBLISHER.getConfigName(),
          new TreeMap<>(
            Map.ofEntries(Map.entry("/events", new TreeSet<>(Set.of("GET")))))),
        Map.entry(
          PortCallRole.SUBSCRIBER.getConfigName(),
          new TreeMap<>(
            Map.ofEntries(
              Map.entry("/events", new TreeSet<>(Set.of("POST")))))));
    return Map.ofEntries(Map.entry(SCENARIO_SUITE_CONFORMANCE, endpointUrisAndMethodsByRoleName));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
    String standardVersion, String scenarioSuite) {
    return new PortCallComponentFactory(getName(), standardVersion, scenarioSuite);
  }

  @Override
  protected Set<String> getExternalPartyRoleNamesAllowingEmptyUrl() {
    return Set.of();
  }
}
