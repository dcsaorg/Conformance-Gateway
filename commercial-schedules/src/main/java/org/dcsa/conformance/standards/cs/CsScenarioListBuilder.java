package org.dcsa.conformance.standards.cs;

import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.*;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.cs.action.CsGetPortSchedulesAction;
import org.dcsa.conformance.standards.cs.action.CsGetRoutingsAction;
import org.dcsa.conformance.standards.cs.action.CsGetVesselSchedulesAction;
import org.dcsa.conformance.standards.cs.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.cs.party.CsFilterParameter;
import org.dcsa.conformance.standards.cs.party.CsRole;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
    CsComponentFactory componentFactory, String publisherPartyName, String subscriberPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalPublisherPartyName.set(publisherPartyName);
    threadLocalSubscriberPartyName.set(subscriberPartyName);
    return Stream.of(
        Map.entry(
          "Point to Point Routings",
          noAction()
            .thenEither(
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,DEPARTURE_END_DATE,ARRIVAL_START_DATE,ARRIVAL_END_DATE),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,DEPARTURE_END_DATE,ARRIVAL_START_DATE,ARRIVAL_END_DATE,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,RECEIPT_TYPE_AT_ORIGIN,DELIVERY_TYPE_AT_DESTINATION),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,DEPARTURE_END_DATE,ARRIVAL_START_DATE,ARRIVAL_END_DATE,RECEIPT_TYPE_AT_ORIGIN,DELIVERY_TYPE_AT_DESTINATION),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,DEPARTURE_END_DATE,ARRIVAL_START_DATE,ARRIVAL_END_DATE,RECEIPT_TYPE_AT_ORIGIN,DELIVERY_TYPE_AT_DESTINATION,MAX_TRANSHIPMENT))),
        Map.entry(
          "Port Schedules",
          noAction()
            .thenEither(
              scenarioWithParametersPs(UN_LOCATION_CODE,DATE))),
        Map.entry(
          "Vessel Schedules - Service",
          noAction()
            .thenEither(
              scenarioWithParametersVsService(CARRIER_SERVICE_CODE),
              scenarioWithParametersVsService(UNIVERSAL_SERVICE_REFERENCE))),
        Map.entry(
          "Vessel Schedules - Vessel",
          noAction()
            .thenEither(
              scenarioWithParametersVsVessel(VESSEL_IMO_NUMBER),
              scenarioWithParametersVsVessel(VESSEL_IMO_NUMBER,CARRIER_SERVICE_CODE,UNIVERSAL_SERVICE_REFERENCE),
              scenarioWithParametersVsVessel(VESSEL_IMO_NUMBER,CARRIER_SERVICE_CODE,UNIVERSAL_SERVICE_REFERENCE,CARRIER_VOYAGE_NUMBER,UNIVERSAL_VOYAGE_REFERENCE))),
        Map.entry(
          "Vessel Schedules - Voyage",
          noAction()
            .thenEither(
              scenarioWithParametersVsVoyage(CARRIER_SERVICE_CODE,CARRIER_VOYAGE_NUMBER),
              scenarioWithParametersVsVoyage(UNIVERSAL_VOYAGE_REFERENCE,CARRIER_SERVICE_CODE, UNIVERSAL_SERVICE_REFERENCE))),
        Map.entry(
          "Vessel Schedules - Location",
          noAction()
            .thenEither(
              scenarioWithParametersVsLocation(UN_LOCATION_CODE),
              scenarioWithParametersVsLocation(UN_LOCATION_CODE, FACILITY_SMDG_CODE))))
      .collect(
        Collectors.toMap(
          Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static CsScenarioListBuilder noAction() {
    return new CsScenarioListBuilder(null);
  }

  private static CsScenarioListBuilder scenarioWithParametersPtp(
    CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters).then(getPtpRoutings());
  }

  private static CsScenarioListBuilder scenarioWithParametersPs(
    CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters).then(getPortSchedules());
  }
  private static CsScenarioListBuilder scenarioWithParametersVsService(
    CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters).then(getVesselSchedules());
  }
  private static CsScenarioListBuilder scenarioWithParametersVsVessel(
    CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters).then(getVesselSchedules());
  }
  private static CsScenarioListBuilder scenarioWithParametersVsVoyage(
    CsFilterParameter... csFilterParameters) {
    return supplyScenarioParameters(csFilterParameters).then(getVesselSchedules());
  }
  private static CsScenarioListBuilder scenarioWithParametersVsLocation(
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

  private static CsScenarioListBuilder getVesselSchedules() {
    CsComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new CsScenarioListBuilder(
      previousAction ->
        new CsGetVesselSchedulesAction(
          subscriberPartyName,
          publisherPartyName,
          previousAction,
          componentFactory.getMessageSchemaValidator("api","VesselSchedule")));
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
          previousAction,
          componentFactory.getMessageSchemaValidator("api","PointToPoint")));
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
          previousAction,
          componentFactory.getMessageSchemaValidator("api","PortSchedule")));
  }

}
