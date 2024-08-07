package org.dcsa.conformance.standards.cs;

import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;

import java.util.*;

public class CsStandard extends AbstractStandard {
  public static final CsStandard INSTANCE = new CsStandard();
  private CsStandard() {
    super("CS");
  }
  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
      Map.ofEntries(
        Map.entry("1.0.0", new TreeSet<>(Set.of("Conformance")))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(String standardVersion, String scenarioSuite) {
    return new CsComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
