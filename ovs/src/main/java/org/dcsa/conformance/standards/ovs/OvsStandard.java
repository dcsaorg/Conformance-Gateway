package org.dcsa.conformance.standards.ovs;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;

public class OvsStandard extends AbstractStandard {
  public static final OvsStandard INSTANCE = new OvsStandard();

  private OvsStandard() {
    super("OVS");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(
            Map.entry("3.0.0", new TreeSet<>(Set.of("Conformance")))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new OvsComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
