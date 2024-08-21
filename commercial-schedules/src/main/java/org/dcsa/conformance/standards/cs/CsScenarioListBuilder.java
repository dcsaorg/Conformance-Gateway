package org.dcsa.conformance.standards.cs;

import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.cs.action.CsGetPortSchedulesAction;
import org.dcsa.conformance.standards.cs.action.CsGetRoutingsAction;
import org.dcsa.conformance.standards.cs.action.CsGetVesselSchedulesAction;
import org.dcsa.conformance.standards.cs.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.cs.party.CsFilterParameter;

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
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,ARRIVAL_START_DATE),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,ARRIVAL_END_DATE),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_END_DATE,ARRIVAL_START_DATE),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_END_DATE,ARRIVAL_END_DATE),

              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,ARRIVAL_START_DATE,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,ARRIVAL_END_DATE,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_END_DATE,ARRIVAL_START_DATE,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_END_DATE,ARRIVAL_END_DATE,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,DEPARTURE_END_DATE,ARRIVAL_START_DATE,ARRIVAL_END_DATE,MAX_TRANSHIPMENT),

              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DELIVERY_TYPE_AT_DESTINATION),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,RECEIPT_TYPE_AT_ORIGIN),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,ARRIVAL_START_DATE,DELIVERY_TYPE_AT_DESTINATION),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,ARRIVAL_END_DATE,DELIVERY_TYPE_AT_DESTINATION),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_END_DATE,ARRIVAL_START_DATE,DELIVERY_TYPE_AT_DESTINATION),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_END_DATE,ARRIVAL_END_DATE,DELIVERY_TYPE_AT_DESTINATION),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,ARRIVAL_START_DATE,RECEIPT_TYPE_AT_ORIGIN),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,ARRIVAL_END_DATE,RECEIPT_TYPE_AT_ORIGIN),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_END_DATE,ARRIVAL_START_DATE,RECEIPT_TYPE_AT_ORIGIN),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_END_DATE,ARRIVAL_END_DATE,RECEIPT_TYPE_AT_ORIGIN),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,DEPARTURE_END_DATE,ARRIVAL_START_DATE,ARRIVAL_END_DATE,RECEIPT_TYPE_AT_ORIGIN),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,DEPARTURE_END_DATE,ARRIVAL_START_DATE,ARRIVAL_END_DATE,DELIVERY_TYPE_AT_DESTINATION),

              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,ARRIVAL_START_DATE,DELIVERY_TYPE_AT_DESTINATION,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,ARRIVAL_END_DATE,DELIVERY_TYPE_AT_DESTINATION,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_END_DATE,ARRIVAL_START_DATE,DELIVERY_TYPE_AT_DESTINATION,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_END_DATE,ARRIVAL_END_DATE,DELIVERY_TYPE_AT_DESTINATION,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,ARRIVAL_START_DATE,RECEIPT_TYPE_AT_ORIGIN,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,ARRIVAL_END_DATE,RECEIPT_TYPE_AT_ORIGIN,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_END_DATE,ARRIVAL_START_DATE,RECEIPT_TYPE_AT_ORIGIN,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_END_DATE,ARRIVAL_END_DATE,RECEIPT_TYPE_AT_ORIGIN,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,DEPARTURE_END_DATE,ARRIVAL_START_DATE,ARRIVAL_END_DATE,RECEIPT_TYPE_AT_ORIGIN,MAX_TRANSHIPMENT),
              scenarioWithParametersPtp(PLACE_OF_DELIVERY,PLACE_OF_RECEIPT,DEPARTURE_START_DATE,DEPARTURE_END_DATE,ARRIVAL_START_DATE,ARRIVAL_END_DATE,DELIVERY_TYPE_AT_DESTINATION,MAX_TRANSHIPMENT),
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
              scenarioWithParametersVs(CARRIER_SERVICE_CODE),
              scenarioWithParametersVs(UNIVERSAL_SERVICE_REFERENCE))),
        Map.entry(
          "Vessel Schedules - Vessel",
          noAction()
            .thenEither(
              scenarioWithParametersVs(VESSEL_IMO_NUMBER))),
        Map.entry(
          "Vessel Schedules - Voyage",
          noAction()
            .thenEither(
              scenarioWithParametersVs(CARRIER_VOYAGE_NUMBER,CARRIER_SERVICE_CODE),
              scenarioWithParametersVs(CARRIER_VOYAGE_NUMBER,UNIVERSAL_SERVICE_REFERENCE),
              scenarioWithParametersVs(UNIVERSAL_VOYAGE_REFERENCE,CARRIER_SERVICE_CODE),
              scenarioWithParametersVs(UNIVERSAL_VOYAGE_REFERENCE,UNIVERSAL_SERVICE_REFERENCE))),
        Map.entry(
          "Vessel Schedules - Location",
          noAction()
            .thenEither(
              scenarioWithParametersVs(UN_LOCATION_CODE),
              scenarioWithParametersVs(UN_LOCATION_CODE, FACILITY_SMDG_CODE))))
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
          componentFactory.getMessageSchemaValidator("api","serviceSchedules")));
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
          componentFactory.getMessageSchemaValidator("api","pointToPointRoutings")));
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
          componentFactory.getMessageSchemaValidator("api","portSchedules")));
  }

}
