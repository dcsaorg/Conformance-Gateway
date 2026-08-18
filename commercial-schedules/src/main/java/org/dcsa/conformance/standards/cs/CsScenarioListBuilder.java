package org.dcsa.conformance.standards.cs;

import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.ARRIVAL_END_DATE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.ARRIVAL_START_DATE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.CARGO_TYPE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.CARRIER_SERVICE_CODE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.CARRIER_VOYAGE_NUMBER;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.DATE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.DELIVERY_TYPE_AT_DESTINATION;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.DEPARTURE_END_DATE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.DEPARTURE_START_DATE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.END_DATE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.FACILITY_SMDG_CODE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.LIMIT;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.MAX_TRANSHIPMENT;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.PLACE_OF_DELIVERY;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.PLACE_OF_RECEIPT;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.RECEIPT_TYPE_AT_ORIGIN;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.RESPONSE_SCOPE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.START_DATE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.UNIVERSAL_SERVICE_REFERENCE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.UNIVERSAL_VOYAGE_REFERENCE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.UN_LOCATION_CODE;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.VESSEL_IMO_NUMBER;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.VESSEL_NAME;
import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.VESSEL_OPERATOR_CARRIER_CODE;

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
  private static final ThreadLocal<String> threadLocalProducerPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalConsumerPartyName = new ThreadLocal<>();

  protected CsScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static LinkedHashMap<String, CsScenarioListBuilder> createModuleScenarioListBuilders(
      CsComponentFactory componentFactory,
      Set<String> testedPartyRoleNames,
      String producerPartyName,
      String consumerPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalProducerPartyName.set(producerPartyName);
    threadLocalConsumerPartyName.set(consumerPartyName);
    Map<String, Map<String, CsScenarioListBuilder>> partyScenariosMap =
        MapUtils.orderedMap(
            Map.entry(
                CsRole.PRODUCER.getConfigName(),
                MapUtils.orderedMap(
                    Map.entry(
                        "Point-to-Point: Required query parameter scenario — Required",
                        scenarioWithParametersPtp(PLACE_OF_RECEIPT, PLACE_OF_DELIVERY)),
                    Map.entry(
                        "Point-to-Point: Optional query parameter scenario — Optional/report-only",
                        scenarioWithRequiredAndOptionalParametersPtp(
                                new CsFilterParameter[] {PLACE_OF_RECEIPT, PLACE_OF_DELIVERY},
                                new CsFilterParameter[] {
                                  DEPARTURE_START_DATE,
                                  DEPARTURE_END_DATE,
                                  ARRIVAL_START_DATE,
                                  ARRIVAL_END_DATE,
                                  MAX_TRANSHIPMENT,
                                  RECEIPT_TYPE_AT_ORIGIN,
                                  DELIVERY_TYPE_AT_DESTINATION,
                                  CARGO_TYPE
                                })
                            .asOptionalReportOnlyScenario()),
                    Map.entry(
                        "Point-to-Point: Pagination scenario — Optional/report-only",
                        scenarioWithParametersPtpForPagination(
                                getPtpRoutings(), PLACE_OF_RECEIPT, PLACE_OF_DELIVERY, LIMIT)
                            .asOptionalReportOnlyScenario()),
                    Map.entry(
                        "Port Schedules: Required query parameter scenario — Required",
                        scenarioWithParametersPs(UN_LOCATION_CODE, DATE)),
                    Map.entry(
                        "Port Schedules: Pagination scenario — Optional/report-only",
                        scenarioWithParametersPsForPagination(
                                getPortSchedules(), UN_LOCATION_CODE, DATE, LIMIT)
                            .asOptionalReportOnlyScenario()),
                    Map.entry(
                        "Vessel Schedules - Service :  Required query parameter scenario",
                        scenarioWithParametersVs(CARRIER_SERVICE_CODE)),
                    Map.entry(
                        "Vessel Schedules - Vessel : Required query parameter scenario",
                        scenarioWithParametersVs(VESSEL_IMO_NUMBER)),
                    Map.entry(
                        "Vessel Schedules -  Voyage : Required query parameter scenario",
                        scenarioWithParametersVs(CARRIER_SERVICE_CODE, CARRIER_VOYAGE_NUMBER)),
                    Map.entry(
                        "Vessel Schedules - Location : Required query parameter scenarios",
                        noAction()
                            .thenEither(
                                scenarioWithParametersVs(UN_LOCATION_CODE),
                                scenarioWithParametersVs(UN_LOCATION_CODE, FACILITY_SMDG_CODE))),
                    Map.entry(
                        "Vessel Schedules: Optional query parameter scenario — Optional/report-only",
                        scenarioWithRequiredAndOptionalParametersVs(
                                new CsFilterParameter[] {CARRIER_SERVICE_CODE},
                                new CsFilterParameter[] {
                                  VESSEL_NAME,
                                  UNIVERSAL_SERVICE_REFERENCE,
                                  UNIVERSAL_VOYAGE_REFERENCE,
                                  VESSEL_OPERATOR_CARRIER_CODE,
                                  START_DATE,
                                  END_DATE,
                                  RESPONSE_SCOPE
                                })
                            .asOptionalReportOnlyScenario()),
                    Map.entry(
                        "Vessel Schedules: Pagination scenario — Optional/report-only",
                        scenarioWithParametersVsForPagination(
                                getVesselSchedules(), CARRIER_SERVICE_CODE, LIMIT)
                            .asOptionalReportOnlyScenario()))),
            Map.entry(
                CsRole.CONSUMER.getConfigName(),
                MapUtils.orderedMap(
                    Map.entry(
                        "Point-to-Point: GET scenario - Required",
                        noAction().then(getPtpRoutings())),
                    Map.entry(
                        "Port Schedules: GET scenario - Required",
                        noAction().then(getPortSchedules())),
                    Map.entry(
                        "Vessel Schedules: GET scenario - Required",
                        noAction().then(getVesselSchedules())))));

    LinkedHashMap<String, CsScenarioListBuilder> scenarios = new LinkedHashMap<>();
    testedPartyRoleNames.forEach(
        party -> scenarios.putAll(partyScenariosMap.getOrDefault(party, Map.of())));

    return scenarios;
  }

  private static CsScenarioListBuilder scenarioWithParametersPtpForPagination(
      CsScenarioListBuilder nextRoutingsAction, CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters)
        .then(getPtpRoutings(true).then(nextRoutingsAction));
  }

  private static CsScenarioListBuilder scenarioWithParametersPsForPagination(
      CsScenarioListBuilder nextPortSchedulesAction, CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters)
        .then(getPortSchedules(true).then(nextPortSchedulesAction));
  }

  private static CsScenarioListBuilder scenarioWithParametersVsForPagination(
      CsScenarioListBuilder nextVesselSchedulesAction, CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters)
        .then(getVesselSchedules(true).then(nextVesselSchedulesAction));
  }

  private static CsScenarioListBuilder scenarioWithParametersPtp(
      CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters).then(getPtpRoutings());
  }

  private static CsScenarioListBuilder scenarioWithRequiredAndOptionalParametersPtp(
      CsFilterParameter[] requiredParams, CsFilterParameter[] optionalParams) {
    return supplyScenarioParametersWithOptional(requiredParams, optionalParams)
        .then(getPtpRoutings());
  }

  private static CsScenarioListBuilder scenarioWithParametersPs(
      CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters).then(getPortSchedules());
  }

  private static CsScenarioListBuilder scenarioWithParametersVs(
      CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters).then(getVesselSchedules());
  }

  private static CsScenarioListBuilder scenarioWithRequiredAndOptionalParametersVs(
      CsFilterParameter[] requiredParams, CsFilterParameter[] optionalParams) {
    return supplyScenarioParametersWithOptional(requiredParams, optionalParams)
        .then(getVesselSchedules(false));
  }

  private static CsScenarioListBuilder supplyScenarioParameters(
      CsFilterParameter... csFilterParameters) {
    String publisherPartyName = threadLocalProducerPartyName.get();
    return new CsScenarioListBuilder(
        previousAction ->
            new SupplyScenarioParametersAction(publisherPartyName, csFilterParameters));
  }

  private static CsScenarioListBuilder supplyScenarioParametersWithOptional(
      CsFilterParameter[] requiredParams, CsFilterParameter[] optionalParams) {
    String publisherPartyName = threadLocalProducerPartyName.get();
    return new CsScenarioListBuilder(
        previousAction ->
            new SupplyScenarioParametersAction(publisherPartyName, requiredParams, optionalParams));
  }

  private static CsScenarioListBuilder noAction() {
    return new CsScenarioListBuilder(null);
  }

  private static CsScenarioListBuilder getVesselSchedules() {
    return getVesselSchedules(false);
  }

  private static CsScenarioListBuilder getVesselSchedules(boolean expectNextPageCursor) {
    CsComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalProducerPartyName.get();
    String subscriberPartyName = threadLocalConsumerPartyName.get();
    return new CsScenarioListBuilder(
        previousAction ->
            new CsGetVesselSchedulesAction(
                subscriberPartyName,
                publisherPartyName,
                (CsAction) previousAction,
                componentFactory.getMessageSchemaValidator("serviceSchedules"),
                expectNextPageCursor));
  }

  private static CsScenarioListBuilder getPtpRoutings() {
    return getPtpRoutings(false);
  }

  private static CsScenarioListBuilder getPtpRoutings(boolean expectNextPageCursor) {
    CsComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalProducerPartyName.get();
    String subscriberPartyName = threadLocalConsumerPartyName.get();
    return new CsScenarioListBuilder(
        previousAction ->
            new CsGetRoutingsAction(
                subscriberPartyName,
                publisherPartyName,
                (CsAction) previousAction,
                componentFactory.getMessageSchemaValidator("pointToPointRoutings"),
                expectNextPageCursor));
  }

  private static CsScenarioListBuilder getPortSchedules() {
    return getPortSchedules(false);
  }

  private static CsScenarioListBuilder getPortSchedules(boolean expectNextPageCursor) {
    CsComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalProducerPartyName.get();
    String subscriberPartyName = threadLocalConsumerPartyName.get();
    return new CsScenarioListBuilder(
        previousAction ->
            new CsGetPortSchedulesAction(
                subscriberPartyName,
                publisherPartyName,
                (CsAction) previousAction,
                componentFactory.getMessageSchemaValidator("portSchedules"),
                expectNextPageCursor));
  }
}
