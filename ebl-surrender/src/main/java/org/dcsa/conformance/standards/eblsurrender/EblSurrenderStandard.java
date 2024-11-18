package org.dcsa.conformance.standards.eblsurrender;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;

public class EblSurrenderStandard extends AbstractStandard {
  public static final EblSurrenderStandard INSTANCE = new EblSurrenderStandard();
  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private EblSurrenderStandard() {
    super("eBL Surrender");
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
                    EblSurrenderRole.CARRIER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            Map.entry(
                                "/v3/ebl-surrender-requests", new TreeSet<>(Set.of("POST")))))),
                Map.entry(
                    EblSurrenderRole.PLATFORM.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            Map.entry(
                                "/v3/ebl-surrender-responses", new TreeSet<>(Set.of("POST")))))))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new EblSurrenderComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
