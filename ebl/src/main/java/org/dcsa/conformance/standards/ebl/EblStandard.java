package org.dcsa.conformance.standards.ebl;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.ebl.party.EblRole;

public class EblStandard extends AbstractStandard {

  public static final Set<String> EBL_ENDPOINT_PATTERNS =
      Set.of(
          ".*/v3/shipping-instructions(?:/[^/]+)?$",
          ".*/v3/transport-documents(?:/[^/]+)?$",
          ".*/v3/shipping-instructions-notifications$",
          ".*/v3/transport-document-notifications$");

  public static final EblStandard INSTANCE = new EblStandard();

  private EblStandard() {
    super("Ebl");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(Map.entry("3.0.0", new TreeSet<>(EblScenarioListBuilder.SCENARIO_SUITES))));
  }

  @Override
  public Map<String, Map<String, SortedMap<String, SortedSet<String>>>>
      getEndpointUrisAndMethodsByScenarioSuiteAndRoleName() {
    return Map.ofEntries(
        Map.entry(
            EblScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE_SI_ONLY,
            Map.ofEntries(
                Map.entry(
                    EblRole.CARRIER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            Map.entry("/v3/shipping-instructions", new TreeSet<>(Set.of("POST"))),
                            Map.entry(
                                "/v3/shipping-instructions/{documentReference}",
                                new TreeSet<>(Set.of("PUT", "GET", "PATCH")))))),
                Map.entry(
                    EblRole.SHIPPER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            Map.entry(
                                "/v3/shipping-instructions-notifications",
                                new TreeSet<>(Set.of("POST")))))))),
        Map.entry(
            EblScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE_TD_ONLY,
            Map.ofEntries(
                Map.entry(
                    EblRole.CARRIER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            Map.entry(
                                "/v3/transport-documents/{transportDocumentReference}",
                                new TreeSet<>(Set.of("GET", "PATCH")))))),
                Map.entry(
                    EblRole.SHIPPER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            Map.entry(
                                "/v3/transport-document-notifications",
                                new TreeSet<>(Set.of("POST")))))))),
        Map.entry(
            EblScenarioListBuilder.SCENARIO_SUITE_SI_TD_COMBINED,
            Map.ofEntries(
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
                            Map.entry(
                                "/v3/shipping-instructions-notifications",
                                new TreeSet<>(Set.of("POST"))),
                            Map.entry(
                                "/v3/transport-document-notifications",
                                new TreeSet<>(Set.of("POST")))))))),
        Map.entry(
            EblScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE_TD_AMENDMENTS,
            Map.ofEntries(
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
                            Map.entry(
                                "/v3/shipping-instructions-notifications",
                                new TreeSet<>(Set.of("POST"))),
                            Map.entry(
                                "/v3/transport-document-notifications",
                                new TreeSet<>(Set.of("POST")))))))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new EblComponentFactory(getName(), standardVersion, scenarioSuite);
  }

  @Override
  protected Set<String> getExternalPartyRoleNamesAllowingEmptyUrl() {
    return Set.of(EblRole.SHIPPER.getConfigName());
  }
}
