package org.dcsa.conformance.standards.ovs;

import static org.dcsa.conformance.standards.ovs.party.OvsFilterParameter.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
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
    LinkedHashMap<String, OvsScenarioListBuilder> scenarioGroups = new LinkedHashMap<>();

    scenarioGroups.put(
        "Schedule Producer: GET scenarios for supported general filtering combinations — Alternative required path",
        noAction()
            .thenEither(
                scenarioWithParameters(parameters(Map.entry(CARRIER_SERVICE_CODE, "BW1"))),
                scenarioWithParameters(
                    parameters(
                        Map.entry(CARRIER_SERVICE_CODE, "BW1"),
                        Map.entry(CARRIER_VOYAGE_NUMBER, "2104N"))),
                scenarioWithParameters(
                    parameters(
                        Map.entry(CARRIER_SERVICE_CODE, "BW1"),
                        Map.entry(VESSEL_IMO_NUMBER, "9456789"))),
                scenarioWithParameters(parameters(Map.entry(VESSEL_IMO_NUMBER, "9456789"))),
                scenarioWithParameters(parameters(Map.entry(UN_LOCATION_CODE, "NLAMS"))),
                scenarioWithParameters(
                    parameters(
                        Map.entry(UN_LOCATION_CODE, "NLAMS"),
                        Map.entry(FACILITY_SMDG_CODE, "APM")))));

    scenarioGroups.put(
        "Schedule Producer: GET scenarios for supported universal-reference filtering combinations — Alternative required path",
        noAction()
            .thenEither(
                scenarioWithParameters(parameters(Map.entry(UNIVERSAL_SERVICE_REFERENCE, "SR12345A"))),
                scenarioWithParameters(
                    parameters(
                        Map.entry(UNIVERSAL_SERVICE_REFERENCE, "SR12345A"),
                        Map.entry(CARRIER_VOYAGE_NUMBER, "2103N"))),
                scenarioWithParameters(
                    parameters(
                        Map.entry(CARRIER_SERVICE_CODE, "FE1"),
                        Map.entry(UNIVERSAL_VOYAGE_REFERENCE, "2103N"))),
                scenarioWithParameters(
                    parameters(
                        Map.entry(UNIVERSAL_SERVICE_REFERENCE, "SR54321C"),
                        Map.entry(UNIVERSAL_VOYAGE_REFERENCE, "2105N")))));

    scenarioGroups.put(
        "Schedule Producer: GET scenario for pagination - Optional/report-only",
        scenarioWithPagination(
            parameters(Map.entry(CARRIER_SERVICE_CODE, "BW1"), Map.entry(LIMIT, "1"))));

    scenarioGroups.put("Schedule Consumer: GET scenario - Required", noAction().then(getSchedules(false)));

    return scenarioGroups;
  }

  private OvsScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static OvsScenarioListBuilder noAction() {
    return new OvsScenarioListBuilder(null);
  }

  private static OvsScenarioListBuilder scenarioWithParameters(Map<OvsFilterParameter, String> parameters) {
    return supplyScenarioParameters(parameters).then(getSchedules(true));
  }

  private static OvsScenarioListBuilder scenarioWithPagination(Map<OvsFilterParameter, String> parameters) {
    return supplyScenarioParameters(parameters).then(getSchedules(true).then(getSchedules(true)));
  }

  private static OvsScenarioListBuilder supplyScenarioParameters(Map<OvsFilterParameter, String> parameters) {
    String publisherPartyName = threadLocalPublisherPartyName.get();
    return new OvsScenarioListBuilder(
        previousAction -> new SupplyScenarioParametersAction(publisherPartyName, parameters));
  }

  @SafeVarargs
  private static Map<OvsFilterParameter, String> parameters(Map.Entry<OvsFilterParameter, String>... entries) {
    var orderedParameters = new LinkedHashMap<OvsFilterParameter, String>();
    for (Map.Entry<OvsFilterParameter, String> entry : entries) {
      orderedParameters.put(entry.getKey(), entry.getValue());
    }
    return orderedParameters;
  }

  private static OvsScenarioListBuilder getSchedules(boolean includeResponseContentChecks) {
    OvsComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new OvsScenarioListBuilder(
        previousAction ->
            new OvsGetSchedulesAction(
                subscriberPartyName,
                publisherPartyName,
                previousAction,
                componentFactory.getMessageSchemaValidator(OvsRole.PRODUCER.getConfigName(), false),
                includeResponseContentChecks));
  }
}
