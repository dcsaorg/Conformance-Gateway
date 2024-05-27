package org.dcsa.conformance.standards.jit;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;

public class JitStandard extends AbstractStandard {
  public static final JitStandard INSTANCE = new JitStandard();

  private JitStandard() {
    super("JIT");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(
            Map.entry("1.2.0", new TreeSet<>(Set.of("Conformance")))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new JitComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
