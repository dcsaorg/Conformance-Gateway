package org.dcsa.conformance.standards.tnt;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;

public class TntStandard extends AbstractStandard {
  public static final TntStandard INSTANCE = new TntStandard();

  private TntStandard() {
    super("TnT");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(
            Map.entry("2.2.0", new TreeSet<>(Set.of("Conformance")))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new TntComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
