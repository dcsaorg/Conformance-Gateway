package org.dcsa.conformance.standards.ebl;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;

public class EblStandard extends AbstractStandard {
  public static final EblStandard INSTANCE = new EblStandard();

  private EblStandard() {
    super("Ebl");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(
            Map.entry(
                "3.0.0",
                new TreeSet<>(
                    Set.of(
                        EblScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE_SI_ONLY,
                        EblScenarioListBuilder.SCENARIO_SUITE_RI)))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new EblComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
