package org.dcsa.conformance.standards.an;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.an.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.an.party.ArrivalNoticeFilterParameter;

@Slf4j
public class ArrivalNoticeScenarioListBuilder extends ScenarioListBuilder<ArrivalNoticeScenarioListBuilder> {
  private static final ThreadLocal<ArrivalNoticeComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  public static LinkedHashMap<String, ArrivalNoticeScenarioListBuilder> createModuleScenarioListBuilders(
    ArrivalNoticeComponentFactory componentFactory, String publisherPartyName, String subscriberPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalPublisherPartyName.set(publisherPartyName);
    threadLocalSubscriberPartyName.set(subscriberPartyName);
    return Stream.of(
            Map.entry(
                "Service schedules",
                noAction()
                    .thenEither(
                        scenarioWithParameters(CARRIER_SERVICE_CODE),
                        scenarioWithParameters(CARRIER_SERVICE_CODE, LIMIT),
                        scenarioWithParameters(UNIVERSAL_SERVICE_REFERENCE),
                        scenarioWithParameters(UNIVERSAL_SERVICE_REFERENCE, LIMIT))),
            Map.entry(
                "Vessel schedules",
                noAction()
                    .thenEither(
                        scenarioWithParameters(VESSEL_IMO_NUMBER),
                        scenarioWithParameters(VESSEL_IMO_NUMBER, LIMIT))),
            Map.entry(
                "Location schedules",
                noAction()
                    .thenEither(
                        scenarioWithParameters(UN_LOCATION_CODE),
                        scenarioWithParameters(UN_LOCATION_CODE, LIMIT),
                        scenarioWithParameters(UN_LOCATION_CODE, FACILITY_SMDG_CODE),
                        scenarioWithParameters(UN_LOCATION_CODE, FACILITY_SMDG_CODE, LIMIT))),
            Map.entry(
                "Voyage schedules",
                noAction()
                    .thenEither(
                        scenarioWithParameters(CARRIER_VOYAGE_NUMBER, CARRIER_SERVICE_CODE),
                        scenarioWithParameters(CARRIER_VOYAGE_NUMBER, CARRIER_SERVICE_CODE, LIMIT),
                        scenarioWithParameters(CARRIER_VOYAGE_NUMBER, UNIVERSAL_SERVICE_REFERENCE),
                        scenarioWithParameters(
                            CARRIER_VOYAGE_NUMBER, UNIVERSAL_SERVICE_REFERENCE, LIMIT),
                        scenarioWithParameters(UNIVERSAL_VOYAGE_REFERENCE, CARRIER_SERVICE_CODE),
                        scenarioWithParameters(
                            UNIVERSAL_VOYAGE_REFERENCE, CARRIER_SERVICE_CODE, LIMIT),
                        scenarioWithParameters(
                            UNIVERSAL_VOYAGE_REFERENCE, UNIVERSAL_SERVICE_REFERENCE),
                        scenarioWithParameters(
                            UNIVERSAL_VOYAGE_REFERENCE, UNIVERSAL_SERVICE_REFERENCE, LIMIT))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private ArrivalNoticeScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static ArrivalNoticeScenarioListBuilder noAction() {
    return new ArrivalNoticeScenarioListBuilder(null);
  }

  private static ArrivalNoticeScenarioListBuilder scenarioWithParameters(
    ArrivalNoticeFilterParameter... ovsFilterParameters) {
    return supplyScenarioParameters(ovsFilterParameters).then(getSchedules());
  }

  private static ArrivalNoticeScenarioListBuilder supplyScenarioParameters(
    ArrivalNoticeFilterParameter... ovsFilterParameters) {
    String publisherPartyName = threadLocalPublisherPartyName.get();
    return new ArrivalNoticeScenarioListBuilder(
        previousAction ->
            new SupplyScenarioParametersAction(publisherPartyName, ovsFilterParameters));
  }

  private static ArrivalNoticeScenarioListBuilder getSchedules() {
    ArrivalNoticeComponentFactory componentFactory = threadLocalComponentFactory.get();
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
