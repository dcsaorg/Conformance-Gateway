package org.dcsa.conformance.standards.ovs;

import static org.dcsa.conformance.standards.ovs.party.OvsFilterParameter.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.ovs.action.OvsGetSchedulesAction;
import org.dcsa.conformance.standards.ovs.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.ovs.party.OvsFilterParameter;
import org.dcsa.conformance.standards.ovs.party.OvsRole;

@Slf4j
class OvsScenarioListBuilder extends ScenarioListBuilder<OvsScenarioListBuilder> {
  private static final ThreadLocal<OvsComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  public static LinkedHashMap<String, OvsScenarioListBuilder> createModuleScenarioListBuilders(
      OvsComponentFactory componentFactory, String publisherPartyName, String subscriberPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalPublisherPartyName.set(publisherPartyName);
    threadLocalSubscriberPartyName.set(subscriberPartyName);
    return Stream.of(
            Map.entry(
                "Service schedules",
                noAction()
                    .thenEither(
                      scenarioWithParameters(Map.of(CARRIER_SERVICE_NAME,"Great Lion Service")),
                      scenarioWithParameters(Map.of(CARRIER_SERVICE_NAME,"Blue Whale Service", LIMIT, "1")),
                      scenarioWithParameters(Map.of(CARRIER_SERVICE_NAME,"Red Falcon Service", START_DATE, "2026-01-01")),
                      scenarioWithParameters(Map.of(CARRIER_SERVICE_NAME,"Great Lion Service", END_DATE, "2021-01-01")),
                      scenarioWithParameters(Map.of(CARRIER_SERVICE_CODE, "BW1")),
                      scenarioWithParameters(Map.of(CARRIER_SERVICE_CODE, "BW1", LIMIT, "1")),
                      scenarioWithParameters(Map.of(UNIVERSAL_SERVICE_REFERENCE, "SR12345A")),
                      scenarioWithParameters(Map.of(UNIVERSAL_SERVICE_REFERENCE, "SR67890B", LIMIT, "1"))
                    )),
            Map.entry(
              "Vessel schedules",
              noAction()
                .thenEither(
                  scenarioWithParameters(Map.of(VESSEL_IMO_NUMBER, "9456789")),
                  scenarioWithParameters(Map.of(VESSEL_IMO_NUMBER, "9876543", LIMIT, "1")))),
            Map.entry(
              "Location schedules",
              noAction()
                .thenEither(
                  scenarioWithParameters(Map.of(UN_LOCATION_CODE, "NLAMS")),
                  scenarioWithParameters(Map.of(UN_LOCATION_CODE, "USNYC", LIMIT, "1")),
                  scenarioWithParameters(Map.of(FACILITY_SMDG_CODE, "APM")),
                  scenarioWithParameters(Map.of(FACILITY_SMDG_CODE, "APM", LIMIT, "1")))),
            Map.entry(
              "Voyage schedules",
              noAction()
                .thenEither(
                  scenarioWithParameters(Map.of(CARRIER_VOYAGE_NUMBER, "2104N", CARRIER_SERVICE_CODE, "BW1")),
                  scenarioWithParameters(Map.of(CARRIER_VOYAGE_NUMBER, "2104S", CARRIER_SERVICE_CODE, "BW1", LIMIT, "1")),
                  scenarioWithParameters(Map.of(CARRIER_VOYAGE_NUMBER, "2103N", UNIVERSAL_SERVICE_REFERENCE, "SR12345A")),
                  scenarioWithParameters(Map.of(CARRIER_VOYAGE_NUMBER, "2103S", UNIVERSAL_SERVICE_REFERENCE, "SR12345A", LIMIT, "1")),
                  scenarioWithParameters(Map.of(UNIVERSAL_VOYAGE_REFERENCE, "2103N", CARRIER_SERVICE_CODE, "FE1")),
                  scenarioWithParameters(Map.of(UNIVERSAL_VOYAGE_REFERENCE, "2103S", CARRIER_SERVICE_CODE, "FE1", LIMIT, "1")),
                  scenarioWithParameters(Map.of(UNIVERSAL_VOYAGE_REFERENCE, "2105N", UNIVERSAL_SERVICE_REFERENCE, "SR54321C")),
                  scenarioWithParameters(Map.of(UNIVERSAL_VOYAGE_REFERENCE, "2105S", UNIVERSAL_SERVICE_REFERENCE, "SR54321C", LIMIT, "1")))))
        .collect(
            Collectors.toMap(
              Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private OvsScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static OvsScenarioListBuilder noAction() {
    return new OvsScenarioListBuilder(null);
  }

  private static OvsScenarioListBuilder scenarioWithParameters(Map<OvsFilterParameter, String> parameters) {
    return supplyScenarioParameters(parameters).then(getSchedules());
  }

  private static OvsScenarioListBuilder supplyScenarioParameters(
    Map<OvsFilterParameter, String> parameters) {
    String publisherPartyName = threadLocalPublisherPartyName.get();
    return new OvsScenarioListBuilder(
      previousAction ->
        new SupplyScenarioParametersAction(publisherPartyName, parameters));
  }


  private static OvsScenarioListBuilder getSchedules() {
    OvsComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new OvsScenarioListBuilder(
        previousAction ->
            new OvsGetSchedulesAction(
                subscriberPartyName,
                publisherPartyName,
                previousAction,
                componentFactory.getMessageSchemaValidator(
                    OvsRole.PUBLISHER.getConfigName(), false)));
  }
}
