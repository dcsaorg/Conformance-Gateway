package org.dcsa.conformance.standards.an;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;

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
    throw new UnsupportedOperationException(); // FIXME
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    throw new UnsupportedOperationException(); // FIXME
  }

  @Override
  protected Set<String> getExternalPartyRoleNamesAllowingEmptyUrl() {
    return Set.of(); // FIXME
  }
}
