package org.dcsa.conformance.standards.eblinterop;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;

public class PintStandard extends AbstractStandard {
  public static final PintStandard INSTANCE = new PintStandard();

  private PintStandard() {
    super("PINT");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(
            Map.entry("3.0.0", new TreeSet<>(Set.of("Reference Implementation")))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new PintComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
