package org.dcsa.conformance.standards.ebl;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.ebl.party.EblRole;

public class EblStandard extends AbstractStandard {
  public static final EblStandard INSTANCE = new EblStandard();

  private EblStandard() {
    super("Ebl");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(Map.entry("3.0.0", new TreeSet<>(EblScenarioListBuilder.SCENARIOS))));
  }

  @Override
  public Map<String, SortedMap<String, SortedSet<String>>> getRoleNameEndpointUriMethods() {
    return Map.ofEntries(
        Map.entry(
            EblRole.CARRIER.getConfigName(),
            new TreeMap<>(
                Map.ofEntries(
                    Map.entry("/v3/shipping-instructions", new TreeSet<>(Set.of("POST"))),
                    Map.entry(
                        "/v3/shipping-instructions/{documentReference}",
                        new TreeSet<>(Set.of("PUT", "GET", "PATCH"))),
                    Map.entry(
                        "/v3/transport-documents/{transportDocumentReference}",
                        new TreeSet<>(Set.of("GET", "PATCH")))))),
        Map.entry(
            EblRole.SHIPPER.getConfigName(),
            new TreeMap<>(
                Map.ofEntries(
                    Map.entry("/v2/booking-notifications", new TreeSet<>(Set.of("POST"))),
                    Map.entry(
                        "/v3/transport-document-notifications", new TreeSet<>(Set.of("POST")))))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new EblComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
