package org.dcsa.conformance.standards.eblsurrender;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;

public class EblSurrenderStandard extends AbstractStandard {
  public static final EblSurrenderStandard INSTANCE = new EblSurrenderStandard();

  private EblSurrenderStandard() {
    super("eBL Surrender");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(Map.ofEntries(Map.entry("3.0.0", new TreeSet<>(Set.of("Conformance")))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new EblSurrenderComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
