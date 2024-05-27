package org.dcsa.conformance.standards.eblissuance;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;

public class EblIssuanceStandard extends AbstractStandard {
  public static final EblIssuanceStandard INSTANCE = new EblIssuanceStandard();

  private EblIssuanceStandard() {
    super("eBL Issuance");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(
            Map.entry("2.0.0", new TreeSet<>(Set.of("Conformance"))),
            Map.entry("3.0.0", new TreeSet<>(Set.of("Conformance")))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new EblIssuanceComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
