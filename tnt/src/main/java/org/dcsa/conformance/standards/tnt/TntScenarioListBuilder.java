package org.dcsa.conformance.standards.tnt;

import static org.dcsa.conformance.standards.tnt.party.TntFilterParameter.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.tnt.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.tnt.action.TntAction;
import org.dcsa.conformance.standards.tnt.action.TntGetEventsAction;
import org.dcsa.conformance.standards.tnt.action.TntGetEventsBadRequestAction;
import org.dcsa.conformance.standards.tnt.party.TntFilterParameter;

@Slf4j
class TntScenarioListBuilder extends ScenarioListBuilder<TntScenarioListBuilder> {
  private static final ThreadLocal<TntComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  public static LinkedHashMap<String, TntScenarioListBuilder> createModuleScenarioListBuilders(
      TntComponentFactory componentFactory, String publisherPartyName, String subscriberPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalPublisherPartyName.set(publisherPartyName);
    threadLocalSubscriberPartyName.set(subscriberPartyName);
    return Stream.of(
            Map.entry(
                "",
                noAction()
                    .thenEither(
                        scenarioWithFilterByDateTimesAnd(EVENT_TYPE),
                        scenarioWithFilterBy(EVENT_CREATED_DATE_TIME),
                        scenarioWithFilterBy(EVENT_CREATED_DATE_TIME_EQ),
                        scenarioWithFilterBy(
                            EVENT_CREATED_DATE_TIME_GT, EVENT_CREATED_DATE_TIME_LT),
                        scenarioWithFilterBy(
                            EVENT_CREATED_DATE_TIME_GT, EVENT_CREATED_DATE_TIME_LTE),
                        scenarioWithFilterBy(
                            EVENT_CREATED_DATE_TIME_GTE, EVENT_CREATED_DATE_TIME_LT),
                        scenarioWithFilterBy(
                            EVENT_CREATED_DATE_TIME_GTE, EVENT_CREATED_DATE_TIME_LTE),
                        scenarioWithFilterBy(SHIPMENT_EVENT_TYPE_CODE),
                        scenarioWithFilterByDateTimesAnd(SHIPMENT_EVENT_TYPE_CODE),
                        scenarioWithFilterBy(DOCUMENT_TYPE_CODE),
                        scenarioWithFilterByDateTimesAnd(DOCUMENT_TYPE_CODE),
                        scenarioWithFilterBy(CARRIER_BOOKING_REFERENCE),
                        scenarioWithFilterByDateTimesAnd(CARRIER_BOOKING_REFERENCE),
                        scenarioWithFilterBy(TRANSPORT_DOCUMENT_REFERENCE),
                        scenarioWithFilterByDateTimesAnd(TRANSPORT_DOCUMENT_REFERENCE),
                        scenarioWithFilterBy(TRANSPORT_EVENT_TYPE_CODE),
                        scenarioWithFilterByDateTimesAnd(TRANSPORT_EVENT_TYPE_CODE),
                        scenarioWithFilterBy(TRANSPORT_CALL_ID),
                        scenarioWithFilterByDateTimesAnd(TRANSPORT_CALL_ID),
                        scenarioWithFilterBy(VESSEL_IMO_NUMBER),
                        scenarioWithFilterByDateTimesAnd(VESSEL_IMO_NUMBER),
                        scenarioWithFilterBy(EXPORT_VOYAGE_NUMBER),
                        scenarioWithFilterByDateTimesAnd(EXPORT_VOYAGE_NUMBER),
                        scenarioWithFilterBy(CARRIER_SERVICE_CODE),
                        scenarioWithFilterByDateTimesAnd(CARRIER_SERVICE_CODE),
                        scenarioWithFilterBy(UN_LOCATION_CODE),
                        scenarioWithFilterByDateTimesAnd(UN_LOCATION_CODE),
                        scenarioWithFilterBy(EQUIPMENT_EVENT_TYPE_CODE),
                        scenarioWithFilterByDateTimesAnd(EQUIPMENT_EVENT_TYPE_CODE),
                        scenarioWithFilterBy(EQUIPMENT_REFERENCE),
                        scenarioWithFilterByDateTimesAnd(EQUIPMENT_REFERENCE),
                        scenarioWithBadRequestFilterBy(true, SHIPMENT_EVENT_TYPE_CODE),
                        scenarioWithBadRequestFilterBy(true, EVENT_TYPE),
                        scenarioWithBadRequestFilterBy(true, DOCUMENT_TYPE_CODE),
                        scenarioWithBadRequestFilterBy(true, EQUIPMENT_EVENT_TYPE_CODE))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private TntScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static TntScenarioListBuilder noAction() {
    return new TntScenarioListBuilder(null);
  }

  private static TntScenarioListBuilder scenarioWithFilterBy(TntFilterParameter parameter1) {
    return supplyScenarioParameters(LIMIT, parameter1).then(getEvents());
  }

  private static TntScenarioListBuilder scenarioWithFilterBy(TntFilterParameter... parameter1) {
    return supplyScenarioParameters(parameter1).then(getEvents());
  }

  private static TntScenarioListBuilder scenarioWithFilterByDateTimesAnd(TntFilterParameter parameter1) {
    return supplyScenarioParameters(LIMIT, EVENT_CREATED_DATE_TIME_GTE, EVENT_CREATED_DATE_TIME_LT, parameter1)
        .then(getEvents());
  }

  private static TntScenarioListBuilder scenarioWithFilterBy(TntFilterParameter parameter1, TntFilterParameter parameter2) {
    return supplyScenarioParameters(LIMIT, parameter1, parameter2).then(getEvents());
  }

  private static TntScenarioListBuilder supplyScenarioParameters(TntFilterParameter... TntFilterParameters) {
    String publisherPartyName = threadLocalPublisherPartyName.get();
    return new TntScenarioListBuilder(
        previousAction ->
            new SupplyScenarioParametersAction(publisherPartyName,TntFilterParameters));
  }

  private static TntScenarioListBuilder supplyScenarioParameters( boolean isBadRequest, TntFilterParameter... TntFilterParameters) {
    String publisherPartyName = threadLocalPublisherPartyName.get();
    return new TntScenarioListBuilder(
      previousAction ->
        new SupplyScenarioParametersAction(isBadRequest, publisherPartyName,TntFilterParameters));
  }

  private static TntScenarioListBuilder scenarioWithBadRequestFilterBy(Boolean isBadRequest, TntFilterParameter parameter1) {
    return supplyScenarioParameters(isBadRequest,parameter1).then(getEventsBadRequest());
  }

  private static TntScenarioListBuilder getEvents() {
    TntComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new TntScenarioListBuilder(
        previousAction ->
            new TntGetEventsAction(
                subscriberPartyName,
                publisherPartyName,
              (TntAction) previousAction,
                componentFactory.getEventSchemaValidators()));
  }

  private static TntScenarioListBuilder getEventsBadRequest() {
    TntComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new TntScenarioListBuilder(
      previousAction ->
        new TntGetEventsBadRequestAction(
          subscriberPartyName,
          publisherPartyName,
          (TntAction) previousAction,
          componentFactory.getEventSchemaValidators()));
  }

}
