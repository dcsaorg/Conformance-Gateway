package org.dcsa.conformance.standards.cs;

import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.PLACE_OF_DELIVERY;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.PLACE_OF_RECEIPT;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.ARRIVAL_END_DATE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.ARRIVAL_START_DATE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.CARGO_TYPE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.DELIVERY_TYPE_AT_DESTINATION;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.DEPARTURE_END_DATE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.DEPARTURE_START_DATE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.MAX_TRANSHIPMENT;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.RECEIPT_TYPE_AT_ORIGIN;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.util.MapUtils;
import org.dcsa.conformance.standards.cs.action.CsAction;
import org.dcsa.conformance.standards.cs.action.CsGetPortSchedulesAction;
import org.dcsa.conformance.standards.cs.action.CsGetRoutingsAction;
import org.dcsa.conformance.standards.cs.action.CsGetVesselSchedulesAction;
import org.dcsa.conformance.standards.cs.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.cs.party.CsFilterParameter;
import org.dcsa.conformance.standards.cs.party.CsRole;

@Slf4j
public class CsScenarioListBuilder extends ScenarioListBuilder<CsScenarioListBuilder> {

  private static final ThreadLocal<CsComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  protected CsScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static LinkedHashMap<String, CsScenarioListBuilder> createModuleScenarioListBuilders(
      CsComponentFactory componentFactory,
      Set<String> testedPartyRoleNames,
      String publisherPartyName,
      String subscriberPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalPublisherPartyName.set(publisherPartyName);
    threadLocalSubscriberPartyName.set(subscriberPartyName);
    Map<String, Map<String, CsScenarioListBuilder>> partyScenariosMap =
        MapUtils.orderedMap(
            Map.entry(
                CsRole.PRODUCER.getConfigName(),
                MapUtils.orderedMap(
                    Map.entry(
                        "Required query parameter scenario — Required",
                        scenarioWithParametersPtp(PLACE_OF_RECEIPT, PLACE_OF_DELIVERY)),
                    Map.entry(
                        "Optional query parameter scenario — Optional/report-only",
                        scenarioWithParametersPtp(
                                PLACE_OF_RECEIPT,
                                PLACE_OF_DELIVERY,
                                DEPARTURE_START_DATE,
                                DEPARTURE_END_DATE,
                                ARRIVAL_START_DATE,
                                ARRIVAL_END_DATE,
                                MAX_TRANSHIPMENT,
                                RECEIPT_TYPE_AT_ORIGIN,
                                DELIVERY_TYPE_AT_DESTINATION,
                                CARGO_TYPE)
                            .asOptionalReportOnlyScenario()))),
            Map.entry(
                CsRole.CONSUMER.getConfigName(),
                MapUtils.orderedMap(
                    Map.entry("GET scenario - Required", noAction().then(getPtpRoutings())))));

    LinkedHashMap<String, CsScenarioListBuilder> scenarios = new LinkedHashMap<>();
    testedPartyRoleNames.forEach(
        party -> scenarios.putAll(partyScenariosMap.getOrDefault(party, Map.of())));

    return scenarios;
  }

  private static CsScenarioListBuilder scenarioWithParametersPtpForPagination(
      CsScenarioListBuilder nextRoutingsAction, CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters).then(getPtpRoutings().then(nextRoutingsAction));
  }

  private static CsScenarioListBuilder scenarioWithParametersPsForPagination(
      CsScenarioListBuilder nextPortSchedulesAction, CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters).then(getPortSchedules().then(nextPortSchedulesAction));
  }

  private static CsScenarioListBuilder scenarioWithParametersVsForPagination(
      CsScenarioListBuilder nextVesselSchedulesAction, CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters).then(getVesselSchedules().then(nextVesselSchedulesAction));
  }

  private static CsScenarioListBuilder scenarioWithParametersPtp(
      CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters).then(getPtpRoutings());
  }

  private static CsScenarioListBuilder scenarioWithParametersPs(
      CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters).then(getPortSchedules());
  }

  private static CsScenarioListBuilder scenarioWithParametersVs(
      CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters).then(getVesselSchedules());
  }

  private static CsScenarioListBuilder supplyScenarioParameters(
      CsFilterParameter... csFilterParameters) {
    String publisherPartyName = threadLocalPublisherPartyName.get();
    return new CsScenarioListBuilder(
        previousAction ->
            new SupplyScenarioParametersAction(publisherPartyName, csFilterParameters));
  }

  private static CsScenarioListBuilder noAction() {
    return new CsScenarioListBuilder(null);
  }

  private static CsScenarioListBuilder getVesselSchedules() {
    CsComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new CsScenarioListBuilder(
        previousAction ->
            new CsGetVesselSchedulesAction(
                subscriberPartyName,
                publisherPartyName,
                (CsAction) previousAction,
                componentFactory.getMessageSchemaValidator("serviceSchedules")));
  }

  private static CsScenarioListBuilder getPtpRoutings() {
    CsComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new CsScenarioListBuilder(
        previousAction ->
            new CsGetRoutingsAction(
                subscriberPartyName,
                publisherPartyName,
                (CsAction) previousAction,
                componentFactory.getMessageSchemaValidator("pointToPointRoutings")));
  }

  private static CsScenarioListBuilder getPortSchedules() {
    CsComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new CsScenarioListBuilder(
        previousAction ->
            new CsGetPortSchedulesAction(
                subscriberPartyName,
                publisherPartyName,
                (CsAction) previousAction,
                componentFactory.getMessageSchemaValidator("portSchedules")));
  }
}
