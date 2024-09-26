package org.dcsa.conformance.standards.adoption;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;

public class AdoptionStandard extends AbstractStandard {
  public static final AdoptionStandard INSTANCE = new AdoptionStandard();
  public static final String ADOPTION_STATS_EXAMPLE = "/standards/adoption/messages/adopt-%s-example.json";

  private AdoptionStandard() {
    super("Adoption");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(
            Map.entry("1.0.0", new TreeSet<>(Set.of("Conformance")))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new AdoptionComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
