package org.dcsa.conformance.standards.eblinterop;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;

public class PintStandard extends AbstractStandard {
  public static final PintStandard INSTANCE = new PintStandard();
  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private PintStandard() {
    super("PINT");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(Map.entry("3.0.0", new TreeSet<>(Set.of(SCENARIO_SUITE_CONFORMANCE)))));
  }

  @Override
  public Map<String, Map<String, SortedMap<String, SortedSet<String>>>>
      getEndpointUrisAndMethodsByScenarioSuiteAndRoleName() {
    return Map.ofEntries(
        Map.entry(
            SCENARIO_SUITE_CONFORMANCE,
            Map.ofEntries(
                Map.entry(
                    PintRole.RECEIVING_PLATFORM.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            Map.entry("/v3/receiver-validation", new TreeSet<>(Set.of("POST"))),
                            Map.entry("/v3/envelopes", new TreeSet<>(Set.of("POST"))),
                            Map.entry(
                                "/v3/envelopes/{envelopeReference}/additional-documents/{documentChecksum}",
                                new TreeSet<>(Set.of("PUT"))),
                            Map.entry(
                                "/v3/envelopes/{envelopeReference}/finish-transfer",
                                new TreeSet<>(Set.of("PUT")))))),
                Map.entry(PintRole.SENDING_PLATFORM.getConfigName(), new TreeMap<>()))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new PintComponentFactory(getName(), standardVersion, scenarioSuite);
  }

  @Override
  protected Set<String> getExternalPartyRoleNamesAllowingEmptyUrl() {
    return Set.of(PintRole.SENDING_PLATFORM.getConfigName());
  }
}
