package org.dcsa.conformance.standards.tnt;

import java.util.*;
import java.util.function.BiFunction;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.tnt.v220.party.TntRole;

public class TntStandard extends AbstractStandard {
  public static final TntStandard INSTANCE = new TntStandard();

  private static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private static final String VERSION_220 = "2.2.0";
  private static final String VERSION_300 = "3.0.0";

  private static final String TNT = "TnT";

  public static final String API_PATH = "/events";
  private static final String API_PATH_V2 = "/v2%s".formatted(API_PATH);
  private static final String API_PATH_V3 = "/v3%s".formatted(API_PATH);

  private static final String GET = "GET";
  private static final String POST = "POST";

  private final Map<String, BiFunction<String, String, AbstractComponentFactory>>
      factoryCreatorsByVersion =
          Map.of(
              VERSION_220,
                  (version, suite) ->
                      new org.dcsa.conformance.standards.tnt.v220.TntComponentFactory(
                          getName(), version, suite),
              VERSION_300,
                  (version, suite) ->
                      new org.dcsa.conformance.standards.tnt.v300.TntComponentFactory(
                          getName(), version, suite));

  private TntStandard() {
    super(TNT);
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(
            Map.entry(VERSION_220, new TreeSet<>(Set.of(SCENARIO_SUITE_CONFORMANCE))),
            Map.entry(VERSION_300, new TreeSet<>(Set.of(SCENARIO_SUITE_CONFORMANCE)))));
  }

  @Override
  public Map<String, Map<String, SortedMap<String, SortedSet<String>>>>
      getEndpointUrisAndMethodsByScenarioSuiteAndRoleName() {
    return Map.ofEntries(
        Map.entry(
            SCENARIO_SUITE_CONFORMANCE,
            Map.ofEntries(
                Map.entry(
                    TntRole.PUBLISHER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(Map.entry(API_PATH_V2, new TreeSet<>(Set.of(GET)))))),
                Map.entry(TntRole.SUBSCRIBER.getConfigName(), new TreeMap<>()),
                Map.entry(
                    org.dcsa.conformance.standards.tnt.v300.party.TntRole.PRODUCER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(Map.entry(API_PATH_V3, new TreeSet<>(Set.of(GET)))))),
                Map.entry(
                    org.dcsa.conformance.standards.tnt.v300.party.TntRole.CONSUMER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(Map.entry(API_PATH_V3, new TreeSet<>(Set.of(POST)))))))));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    BiFunction<String, String, AbstractComponentFactory> factoryCreator =
        factoryCreatorsByVersion.get(standardVersion);
    return factoryCreator.apply(standardVersion, scenarioSuite);
  }

  @Override
  protected Set<String> getExternalPartyRoleNamesAllowingEmptyUrl() {
    return Set.of(TntRole.SUBSCRIBER.getConfigName());
  }
}
