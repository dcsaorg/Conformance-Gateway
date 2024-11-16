package org.dcsa.conformance.standards.eblissuance;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

public class EblIssuanceStandard extends AbstractStandard {
  public static final EblIssuanceStandard INSTANCE = new EblIssuanceStandard();
  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private EblIssuanceStandard() {
    super("eBL Issuance");
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
                    EblIssuanceRole.CARRIER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            Map.entry(
                                "/v3/ebl-issuance-responses", new TreeSet<>(Set.of("POST")))))),
                Map.entry(
                    EblIssuanceRole.PLATFORM.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            Map.entry(
                                "/v3/ebl-issuance-requests", new TreeSet<>(Set.of("PUT")))))))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new EblIssuanceComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
