package org.dcsa.conformance.standards.adoption;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.adoption.party.AdoptionRole;

public class AdoptionStandard extends AbstractStandard {
  public static final AdoptionStandard INSTANCE = new AdoptionStandard();
  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";
  public static final String ADOPTION_STATS_EXAMPLE =
      "/standards/adoption/messages/adoption-%s-example.json";

  private AdoptionStandard() {
    super("Adoption");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(Map.entry("1.0.0", new TreeSet<>(Set.of(SCENARIO_SUITE_CONFORMANCE)))));
  }

  @Override
  public Map<String, Map<String, SortedMap<String, SortedSet<String>>>>
      getEndpointUrisAndMethodsByScenarioSuiteAndRoleName() {
    return Map.ofEntries(
        Map.entry(
            SCENARIO_SUITE_CONFORMANCE,
            Map.ofEntries(
                Map.entry(
                    AdoptionRole.ADOPTER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            Map.entry("/v1/adoption-stats", new TreeSet<>(Set.of("GET")))))),
                Map.entry(
                    AdoptionRole.DCSA.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            Map.entry("/v1/adoption-stats", new TreeSet<>(Set.of("GET")))))))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new AdoptionComponentFactory(getName(), standardVersion, scenarioSuite);
  }

  @Override
  protected Set<String> getExternalPartyRoleNamesAllowingEmptyUrl() {
    return Set.of(AdoptionRole.ADOPTER.getConfigName(), AdoptionRole.DCSA.getConfigName());
  }
}
